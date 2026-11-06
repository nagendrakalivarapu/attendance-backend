package com.smartattendance.controller;

import com.smartattendance.dto.auth.LoginRequest;
import com.smartattendance.dto.auth.LoginResponse;
import com.smartattendance.dto.auth.RegisterRequest;
import com.smartattendance.dto.auth.RegisterResponse;
import com.smartattendance.dto.auth.ResendOtpRequest;
import com.smartattendance.dto.auth.VerifyOtpRequest;
import com.smartattendance.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        RegisterResponse response =
                authService.register(request);

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<LoginResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {

        return ResponseEntity.ok(
                authService.verifyOtp(
                        request.getEmail(),
                        request.getOtp()
                )
        );
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request
    ) {

        authService.resendOtp(
                request.getEmail()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "A new OTP has been sent to your registered email."
                )
        );
    }
}