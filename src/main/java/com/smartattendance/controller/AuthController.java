package com.smartattendance.controller;

import com.smartattendance.dto.auth.LoginRequest;
import com.smartattendance.dto.auth.LoginResponse;
import com.smartattendance.dto.auth.ResendOtpRequest;
import com.smartattendance.dto.auth.VerifyOtpRequest;
import com.smartattendance.security.LoginRateLimiter;
import com.smartattendance.security.OtpRateLimiter;
import com.smartattendance.service.AuthService;
import com.smartattendance.service.TokenRevocationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.time.Duration;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String ACCESS_COOKIE = "ACCESS_TOKEN";

    private final AuthService authService;
    private final OtpRateLimiter otpRateLimiter;

    private final LoginRateLimiter loginRateLimiter;

    private final TokenRevocationService tokenRevocationService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    // Login.
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        loginRateLimiter.checkAndRecord(
                request.getEmail(),
                httpRequest.getRemoteAddr()
        );

        LoginResponse response = authService.login(request);

        return createAuthenticatedResponse(response);
    }

    // OTP verification.
    @PostMapping("/verify-otp")
    public ResponseEntity<LoginResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest
    ) {

        otpRateLimiter.checkVerification(
                httpRequest.getRemoteAddr()
        );

        LoginResponse response = authService.verifyOtp(
                request.getEmail(),
                request.getOtp()
        );

        return createAuthenticatedResponse(response);
    }

    // Resend OTP.
    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest
    ) {

        otpRateLimiter.checkResend(
                httpRequest.getRemoteAddr()
        );

        authService.resendOtp(request.getEmail());

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "A new OTP has been sent to your registered email."
                )
        );
    }

    // Get CSRF token.
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(
            CsrfToken csrfToken) {

        return ResponseEntity.ok(
                Map.of("token", csrfToken.getToken())
        );
    }

    // Get currently authenticated user.
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> currentUser(
            Authentication authentication) {

        String role = authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(authority ->
                        authority.getAuthority().replace("ROLE_", ""))
                .orElse("");

        return ResponseEntity.ok(Map.of(
                "email", authentication.getName(),
                "role", role
        ));
    }

    // Logout, revoke JWT and clear authentication cookie.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request
    ) {

        // Read the current ACCESS_TOKEN cookie.
        String token = null;

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_COOKIE.equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // Revoke the current JWT.
        if (token != null && !token.isBlank()) {
            tokenRevocationService.revokeToken(token);
        }

        // Clear the browser cookie.
        ResponseCookie cookie = ResponseCookie.from(
                        ACCESS_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api")
                .sameSite(sameSite)
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    // Create authentication cookie after successful login or OTP.
    private ResponseEntity<LoginResponse> createAuthenticatedResponse(
            LoginResponse response) {

        String token = response.getToken();

        // OTP is still pending for admin/teacher login.
        if (token == null || token.isBlank()) {
            return ResponseEntity.ok(response);
        }

        ResponseCookie cookie = ResponseCookie.from(
                        ACCESS_COOKIE, token)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api")
                .sameSite(sameSite)
                .maxAge(Duration.ofMillis(jwtExpiration))
                .build();

        // Never expose JWT in the response JSON.
        response.setToken(null);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}
