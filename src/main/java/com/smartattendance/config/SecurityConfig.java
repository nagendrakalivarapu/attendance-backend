package com.smartattendance.config;

import com.smartattendance.security.jwt.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // Enable CSRF protection for cookie-based authentication.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                CookieCsrfTokenRepository.withHttpOnlyFalse()
                        )
                        .csrfTokenRequestHandler(
                                new CsrfTokenRequestAttributeHandler()
                        )
                )

                // CORS configuration.
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))

                // Stateless REST API.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Authentication and authorization.
                .authorizeHttpRequests(auth -> auth

                        // Browser preflight requests.
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Public authentication endpoints.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/verify-otp",
                                "/api/auth/resend-otp"
                        ).permitAll()

                        // CSRF token endpoint.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/csrf"
                        ).permitAll()

                        // Public logout endpoint.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/logout"
                        ).permitAll()

                        // Public profile picture viewing only.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/uploads/profile-pictures/**"
                        ).permitAll()

                        // Administrator APIs.
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        // Student creation: admin only.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/student",
                                "/api/student/**"
                        ).hasRole("ADMIN")

                        // Teacher creation: admin only.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/teacher",
                                "/api/teacher/**"
                        ).hasRole("ADMIN")

                        // Everything else requires authentication.
                        .anyRequest().authenticated()
                )

                // Return HTTP 401 for unauthenticated requests.
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                )
                        )
                )

                // Register JWT filter.
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // CORS configuration.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        // Exact frontend origin from application.properties.
        configuration.setAllowedOrigins(
                List.of(frontendUrl)
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // Allow CSRF header.
        configuration.setAllowedHeaders(
                List.of(
                        "Content-Type",
                        "Accept",
                        "X-XSRF-TOKEN"
                )
        );

        // Required for HttpOnly authentication cookies.
        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    // Authentication manager.
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }
}
