package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.PasswordResetToken;
import com.example.codecombat2026.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Transactional
    void deleteByExpiryDateBefore(LocalDateTime date);

    @Transactional
    void deleteByUser(User user);
}
