package com.lcorp.backend.repository;

import com.lcorp.backend.domain.EmailLoginChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmailLoginChallengeRepository extends JpaRepository<EmailLoginChallenge, String> {
    @Modifying
    @Query(value = """
        INSERT INTO public.email_login_challenges (email, auth_version, enrollment, code_hash, issued_at, attempts_left)
        VALUES (:email, -1, false, '', CURRENT_TIMESTAMP, 0)
        ON CONFLICT (email) DO NOTHING
        """, nativeQuery = true)
    void ensureExists(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select challenge from EmailLoginChallenge challenge where challenge.email = :email")
    Optional<EmailLoginChallenge> findLocked(String email);
}
