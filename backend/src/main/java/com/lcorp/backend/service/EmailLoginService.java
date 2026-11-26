package com.lcorp.backend.service;

import com.lcorp.backend.domain.EmailLoginChallenge;
import com.lcorp.backend.domain.UserAccount;
import com.lcorp.backend.dto.AuthDtos.EmailAuthMode;
import com.lcorp.backend.error.AuthFailure;
import com.lcorp.backend.repository.EmailLoginChallengeRepository;
import com.lcorp.backend.repository.UserAccountRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class EmailLoginService {
    private static final Logger LOG = LoggerFactory.getLogger(EmailLoginService.class);
    private static final Duration CODE_LIFETIME = Duration.ofMinutes(5);
    private static final Duration RESEND_INTERVAL = Duration.ofMinutes(1);
    private static final String HMAC = "HmacSHA256";

    private final UserAccountRepository users;
    private final EmailLoginChallengeRepository challenges;
    private final MailSender mail;
    private final ApplicationEventPublisher events;
    private final String from;
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();
    private final ThreadPoolExecutor delivery = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(16), Thread.ofPlatform().name("email-login-", 0).factory());

    public EmailLoginService(UserAccountRepository users, EmailLoginChallengeRepository challenges,
                             MailSender mail, ApplicationEventPublisher events,
                             @Value("${app.auth.email-from}") String from,
                             @Value("${app.jwt.secret-base64}") String secret) {
        this.users = users;
        this.challenges = challenges;
        this.mail = mail;
        this.events = events;
        this.from = from;
        this.key = new SecretKeySpec(Base64.getDecoder().decode(secret), HMAC);
    }

    public boolean enabled() {
        return !from.isBlank();
    }

    @Transactional
    public EmailAuthMode start(String email) {
        requireEnabled();
        String address = normalize(email);
        var account = users.findForLogin(address);
        if (account.isEmpty()) return EmailAuthMode.SIGN_UP;

        UserAccount user = account.get();
        boolean codeAvailable = eligible(user, false, Instant.now());
        if (user.getPasswordHash() != null) {
            return codeAvailable ? EmailAuthMode.PASSWORD : EmailAuthMode.PASSWORD_ONLY;
        }
        if (!codeAvailable) return EmailAuthMode.UNAVAILABLE;

        issue(address, user, false);
        return EmailAuthMode.CODE;
    }

    @Transactional
    public void requestLogin(String email) {
        requireEnabled();
        String address = normalize(email);
        issue(address, users.findForLogin(address).orElse(null), false);
    }

    @Transactional
    public void requestEnrollment(UUID userId) {
        requireEnabled();
        UserAccount user = users.findLocked(userId).orElseThrow(EmailLoginService::invalidCode);
        issue(normalize(user.getEmail()), user, true);
    }

    @Transactional(noRollbackFor = AuthFailure.class)
    public void confirmEnrollment(UUID userId, String code) {
        requireEnabled();
        UserAccount user = users.findLocked(userId).orElseThrow(EmailLoginService::invalidCode);
        Instant now = Instant.now();
        verify(normalize(user.getEmail()), user, code, true, now);
        user.verifyEmail(now);
    }

    @Transactional(propagation = Propagation.MANDATORY, noRollbackFor = AuthFailure.class)
    public Optional<UserAccount> verifyLogin(String email, String code, Instant now) {
        requireEnabled();
        String address = normalize(email);
        var user = users.findForLogin(address);
        verify(address, user.orElse(null), code, false, now);
        return user;
    }

    private void issue(String email, UserAccount user, boolean enrollment) {
        Instant now = Instant.now();
        if (!eligible(user, enrollment, now)) return;
        UUID userId = user == null ? null : user.getId();
        long authVersion = user == null ? 0 : user.getAuthVersion();
        challenges.ensureExists(email);
        var challenge = challenges.findLocked(email).orElseThrow();
        if (challenge.matches(userId, authVersion, enrollment)
            && challenge.getIssuedAt().plus(RESEND_INTERVAL).isAfter(now)) return;

        String code;
        String codeHash;
        do {
            code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            codeHash = HexFormat.of().formatHex(digest(email, userId, code));
        } while (codeHash.equals(challenge.getCodeHash()));
        challenge.issue(userId, authVersion, enrollment, codeHash, now);
        events.publishEvent(new CodeIssued(email, code, enrollment, now.plus(CODE_LIFETIME)));
    }

    private void verify(String email, UserAccount user, String code, boolean enrollment, Instant now) {
        if (!eligible(user, enrollment, now)) throw invalidCode();
        UUID userId = user == null ? null : user.getId();
        long authVersion = user == null ? 0 : user.getAuthVersion();
        var challenge = challenges.findLocked(email).orElseThrow(EmailLoginService::invalidCode);
        if (!challenge.matches(userId, authVersion, enrollment) || !challenge.hasAttempts()
            || !challenge.getIssuedAt().plus(CODE_LIFETIME).isAfter(now)) {
            throw invalidCode();
        }
        byte[] expected = HexFormat.of().parseHex(challenge.getCodeHash());
        if (!MessageDigest.isEqual(expected, digest(email, userId, code))) {
            challenge.failAttempt();
            throw invalidCode();
        }
        challenge.consume();
    }

    private static boolean eligible(UserAccount user, boolean enrollment, Instant now) {
        if (user == null) return !enrollment;
        return user.isEnabled() && !user.isPasswordChangeRequired() && (!enrollment || !user.isEmailVerified())
            && (user.getLockedUntil() == null || !user.getLockedUntil().isAfter(now));
    }

    private byte[] digest(String email, UUID userId, String code) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(key);
            String data = "email-login:" + (userId == null ? email : userId) + ":" + code;
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot hash email login code", ex);
        }
    }

    @TransactionalEventListener
    public void deliver(CodeIssued event) {
        try {
            delivery.execute(() -> send(event));
        } catch (RejectedExecutionException ex) {
            LOG.warn("Email login delivery queue is full");
        }
    }

    private void send(CodeIssued event) {
        if (!event.expiresAt().isAfter(Instant.now())) return;
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(event.email());
        message.setSubject(event.enrollment() ? "Подтверждение почты" : "Код входа в систему");
        message.setText("Код: " + event.code() + "\nКод действует 5 минут.");
        try {
            mail.send(message);
        } catch (MailException ex) {
            LOG.warn("Email login delivery failed ({})", ex.getClass().getSimpleName());
        }
    }

    @PreDestroy
    void stopDelivery() {
        delivery.shutdownNow();
    }

    private void requireEnabled() {
        if (!enabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static AuthFailure invalidCode() {
        return new AuthFailure("Неверный или просроченный код");
    }

    public record CodeIssued(String email, String code, boolean enrollment, Instant expiresAt) {
    }
}
