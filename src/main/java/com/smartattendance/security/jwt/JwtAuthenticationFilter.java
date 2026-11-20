
package com.smartattendance.security.jwt;

import com.smartattendance.security.service.CustomUserDetailsService;
import com.smartattendance.service.TokenRevocationService;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
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

    private static final String ACCESS_COOKIE = "ACCESS_TOKEN";

    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;

    private final TokenRevocationService tokenRevocationService;

    /**
     * Skip JWT authentication for public authentication endpoints.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        return path.equals("/api/auth/login")
                || path.equals("/api/auth/verify-otp")
                || path.equals("/api/auth/resend-otp")
                || path.equals("/api/auth/csrf")
                || path.equals("/api/auth/logout");
    }

    /**
     * Extract and validate JWT from the HttpOnly cookie.
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

        // Read JWT from ACCESS_TOKEN cookie.
        String token = extractTokenFromCookie(request);

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            // Extract username/email from JWT.
            String email = jwtService.extractUsername(token);

            if (email == null || email.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            // Check whether this JWT was revoked during logout.
            if (tokenRevocationService.isTokenRevoked(token)) {

                SecurityContextHolder.clearContext();

                log.debug(
                        "Revoked JWT rejected for request: {}",
                        request.getServletPath()
                );

                filterChain.doFilter(request, response);
                return;
            }

            // Load user from database.
            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

            // Validate JWT and account status.
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

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
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
     * Extract ACCESS_TOKEN from request cookies.
     */
    private String extractTokenFromCookie(
            HttpServletRequest request
    ) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {

            if (ACCESS_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
