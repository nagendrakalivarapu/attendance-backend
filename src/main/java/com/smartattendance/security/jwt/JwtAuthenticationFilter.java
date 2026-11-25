
package com.smartattendance.security.jwt;

import com.smartattendance.security.service.CustomUserDetailsService;
import com.smartattendance.service.impl.TokenRevocationService;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final TokenRevocationService tokenRevocationService;

    /**
     * Skip JWT authentication only for public authentication endpoints.
     * Logout is intentionally NOT skipped.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        return path.equals("/api/auth/login")
                || path.equals("/api/auth/verify-otp")
                || path.equals("/api/auth/resend-otp");
    }

    /**
     * Authenticate requests using Authorization: Bearer <JWT>.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Do not overwrite an existing authentication.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractBearerToken(request);

        // No bearer token: continue without authentication.
        // Protected endpoints will be rejected by Spring Security.
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            // Reject tokens revoked during logout.
            if (tokenRevocationService.isTokenRevoked(token)) {

                SecurityContextHolder.clearContext();

                log.debug(
                        "Revoked JWT rejected for request: {}",
                        request.getServletPath()
                );

                filterChain.doFilter(request, response);
                return;
            }

            // Extract username/email from the signed JWT.
            String email = jwtService.extractUsername(token);

            if (email == null || email.isBlank()) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // Load the account and its authorities from the database.
            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

            // Validate JWT signature, expiry, subject and account status.
            if (jwtService.isTokenValid(token, userDetails)) {

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContext context =
                        SecurityContextHolder.createEmptyContext();

                context.setAuthentication(authentication);

                SecurityContextHolder.setContext(context);

            } else {
                SecurityContextHolder.clearContext();

                log.debug(
                        "Invalid JWT rejected for request: {}",
                        request.getServletPath()
                );
            }

        } catch (JwtException
                 | IllegalArgumentException
                 | AuthenticationException exception) {

            SecurityContextHolder.clearContext();

            log.debug(
                    "JWT authentication failed for request: {}",
                    request.getServletPath()
            );
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract bearer JWT from the Authorization header.
     */
    private String extractBearerToken(HttpServletRequest request) {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorizationHeader
                .substring(BEARER_PREFIX.length())
                .trim();

        return token.isEmpty() ? null : token;
    }
}
