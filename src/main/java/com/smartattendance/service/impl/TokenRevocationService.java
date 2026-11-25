package com.smartattendance.service.impl;

import com.smartattendance.entity.RevokedToken;
import com.smartattendance.repository.RevokedTokenRepository;
import com.smartattendance.security.jwt.JwtService;
import io.jsonwebtoken.JwtException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtService jwtService;

    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable.",
                    exception
            );
        }
    }

    @Transactional
    public void revokeToken(String token) {

        if (token == null || token.isBlank()) {
            return;
        }

        try {
            Instant expiration = jwtService
                    .extractExpiration(token)
                    .toInstant();

            // No need to store already expired tokens.
            if (!expiration.isAfter(Instant.now())) {
                return;
            }

            String tokenHash = hashToken(token);

            if (!revokedTokenRepository.existsByTokenHash(tokenHash)) {

                RevokedToken revokedToken =
                        new RevokedToken(tokenHash, expiration);

                revokedTokenRepository.save(revokedToken);
            }

        } catch (JwtException | IllegalArgumentException exception) {
            // Invalid tokens are not stored.
        }
    }

    @Transactional(readOnly = true)
    public boolean isTokenRevoked(String token) {

        if (token == null || token.isBlank()) {
            return false;
        }

        return revokedTokenRepository.existsByTokenHash(
                hashToken(token)
        );
    }

    @Transactional
    public long removeExpiredTokens() {

        return revokedTokenRepository.deleteAllByExpiresAtBefore(
                Instant.now()
        );
    }
}
