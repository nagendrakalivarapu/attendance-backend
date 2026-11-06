package com.smartattendance.service.impl;

import com.smartattendance.dto.auth.LoginRequest;
import com.smartattendance.dto.auth.LoginResponse;
import com.smartattendance.dto.auth.RegisterRequest;
import com.smartattendance.dto.auth.RegisterResponse;
import com.smartattendance.entity.User;
import com.smartattendance.enums.UserRole;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.service.AuthService;
import com.smartattendance.service.OtpService;
import com.smartattendance.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpService otpService;

    @Override
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .role(request.getRole())
                .enabled(true)
                .build();

        User savedUser =
                userRepository.save(user);

        return RegisterResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .message("Registration Successful")
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user =
                userRepository
                        .findByEmail(request.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new RuntimeException(
                    "Account is disabled"
            );
        }

        /*
         * ADMIN LOGIN
         *
         * Password is correct, but JWT is NOT
         * issued yet. OTP must be verified first.
         */
        if (user.getRole() == UserRole.ADMIN) {

            otpService.generateAndSendOtp(
                    user.getEmail()
            );

            return LoginResponse.builder()
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .token(null)
                    .message(
                            "OTP sent to your registered email."
                    )
                    .requiresOtp(true)
                    .build();
        }

        /*
         * STUDENT / TEACHER LOGIN
         *
         * Existing password → JWT flow remains unchanged.
         */
        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build();

        String token =
                jwtService.generateToken(
                        userDetails
                );

        return LoginResponse.builder()
                .email(user.getEmail())
                .role(user.getRole().name())
                .token(token)
                .message("Login Successful")
                .requiresOtp(false)
                .build();
    }

    @Override
    public LoginResponse verifyOtp(
            String email,
            String otp
    ) {

        otpService.verifyOtp(
                email,
                otp
        );

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        if (user.getRole() != UserRole.ADMIN) {
            throw new RuntimeException(
                    "OTP verification is only available for administrators."
            );
        }

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build();

        String token =
                jwtService.generateToken(
                        userDetails
                );

        return LoginResponse.builder()
                .email(user.getEmail())
                .role(user.getRole().name())
                .token(token)
                .message("Admin login successful.")
                .requiresOtp(false)
                .build();
    }

    @Override
    public void resendOtp(String email) {

        otpService.resendOtp(email);
    }
}