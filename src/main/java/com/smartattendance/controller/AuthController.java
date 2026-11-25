
package com.smartattendance.controller;

import com.smartattendance.dto.auth.LoginRequest;
import com.smartattendance.dto.auth.LoginResponse;
import com.smartattendance.dto.auth.ResendOtpRequest;
import com.smartattendance.dto.auth.VerifyOtpRequest;
import com.smartattendance.security.LoginRateLimiter;
import com.smartattendance.security.OtpRateLimiter;
import com.smartattendance.service.AuthService;
import com.smartattendance.service.impl.TokenRevocationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final OtpRateLimiter otpRateLimiter;
    private final LoginRateLimiter loginRateLimiter;
    private final TokenRevocationService tokenRevocationService;

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

        // Student receives a JWT immediately.
        // Admin and Teacher receive requiresOtp=true and no token yet.
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
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

        // Successful OTP verification returns the JWT in JSON.
        LoginResponse response = authService.verifyOtp(
                request.getEmail(),
                request.getOtp()
        );

        return ResponseEntity.ok(response);
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

    // Get currently authenticated user.
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> currentUser(
            Authentication authentication
    ) {

        String role = authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(authority ->
                        authority.getAuthority().replace("ROLE_", ""))
                .orElse("");

        return ResponseEntity.ok(
                Map.of(
                        "email", authentication.getName(),
                        "role", role
                )
        );
    }

    // Logout and revoke the current bearer JWT.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorizationHeader
    ) {

        if (authorizationHeader != null
                && authorizationHeader.startsWith(BEARER_PREFIX)) {

            String token = authorizationHeader
                    .substring(BEARER_PREFIX.length())
                    .trim();

            if (!token.isBlank()) {
                tokenRevocationService.revokeToken(token);
            }
        }

        return ResponseEntity.noContent().build();
    }
}
