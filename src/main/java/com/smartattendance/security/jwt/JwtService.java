
package com.smartattendance.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long jwtExpiration;

    public JwtService(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration}") long jwtExpiration
    ) {

        if (secretKey == null ||
                secretKey.getBytes(StandardCharsets.UTF_8).length < 32) {

            throw new IllegalStateException(
                    "JWT secret must contain at least 32 bytes."
            );
        }

        if (jwtExpiration <= 0) {
            throw new IllegalStateException(
                    "JWT expiration must be greater than zero."
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );

        this.jwtExpiration = jwtExpiration;
    }

    // Generate JWT token.
    public String generateToken(UserDetails userDetails) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(now.plusMillis(jwtExpiration))
                )
                .signWith(signingKey)
                .compact();
    }

    // Extract username.
    public String extractUsername(String token) {

        return extractClaim(token, Claims::getSubject);
    }

    // Extract expiration.
    public Date extractExpiration(String token) {

        return extractClaim(token, Claims::getExpiration);
    }

    // Extract individual claims.
    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {

        Claims claims = extractAllClaims(token);

        return claimsResolver.apply(claims);
    }

    // Parse and verify signed JWT.
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Validate JWT and user account status.
    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {

        Claims claims = extractAllClaims(token);

        String username = claims.getSubject();

        Date expiration = claims.getExpiration();

        if (username == null || expiration == null) {
            return false;
        }

        return username.equals(userDetails.getUsername())

                && expiration.after(new Date())

                && userDetails.isEnabled()

                && userDetails.isAccountNonLocked()

                && userDetails.isAccountNonExpired()

                && userDetails.isCredentialsNonExpired();
    }
}
