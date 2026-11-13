package com.lcorp.backend.repository;

import com.lcorp.backend.domain.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where lower(u.email) = lower(:email)")
    Optional<UserAccount> findForLogin(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.id = :id")
    Optional<UserAccount> findLocked(UUID id);
}
