package com.lcorp.backend.config;

import com.lcorp.backend.domain.Role;
import com.lcorp.backend.domain.UserAccount;
import com.lcorp.backend.repository.UserAccountRepository;
import com.lcorp.backend.security.PasswordPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final PasswordPolicy policy;
    private final String email;
    private final String name;
    private final String password;

    public BootstrapAdmin(UserAccountRepository users, PasswordEncoder encoder, PasswordPolicy policy,
                          @Value("${app.bootstrap.email}") String email,
                          @Value("${app.bootstrap.name}") String name,
                          @Value("${app.bootstrap.password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.policy = policy;
        this.email = email;
        this.name = name;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;
        if (email.isBlank() || name.isBlank()) {
            throw new IllegalStateException("Bootstrap admin email and name are required for an empty database");
        }
        policy.validate(password);
        users.save(new UserAccount(UUID.randomUUID(), email.trim().toLowerCase(Locale.ROOT),
            name.trim(), encoder.encode(password), Set.of(Role.ADMIN)));
    }
}
