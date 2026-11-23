package com.smartattendance.repository;

import com.smartattendance.entity.RevokedToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface RevokedTokenRepository
        extends JpaRepository<RevokedToken, Long> {

    boolean existsByTokenHash(String tokenHash);

    long deleteAllByExpiresAtBefore(Instant cutoff);
}
