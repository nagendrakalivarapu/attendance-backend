package com.smartattendance.service.impl;

import com.smartattendance.dto.auth.LoginRequest;
import com.smartattendance.dto.auth.LoginResponse;
import com.smartattendance.dto.auth.RegisterRequest;
import com.smartattendance.dto.auth.RegisterResponse;
import com.smartattendance.entity.User;
import com.smartattendance.enums.UserRole;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.security.jwt.JwtService;
import com.smartattendance.service.AuthService;
import com.smartattendance.service.OtpService;
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


    // ============================================================
    // REGISTER
    // ============================================================

    @Override
    public RegisterResponse register(RegisterRequest request) {

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {

            throw new RuntimeException(
                    "Email already exists"
            );
        }

        // Create user
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

        // Save user
        User savedUser =
                userRepository.save(user);

        // Return response
        return RegisterResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .message("Registration Successful")
                .build();
    }


    // ============================================================
    // LOGIN
    // ============================================================

    @Override
    public LoginResponse login(LoginRequest request) {

        /*
         * STEP 1
         *
         * Authenticate email + password.
         *
         * If password is incorrect,
         * AuthenticationManager will reject the request.
         */
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );


        /*
         * STEP 2
         *
         * Load user from database.
         */
        User user =
                userRepository
                        .findByEmail(request.getEmail())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );


        /*
         * STEP 3
         *
         * Check whether account is enabled.
         */
        if (!Boolean.TRUE.equals(
                user.getEnabled()
        )) {

            throw new RuntimeException(
                    "Account is disabled"
            );
        }


        /*
         * STEP 4
         *
         * ADMIN and TEACHER require OTP.
         *
         * STUDENT does not require OTP.
         */
        if (user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.TEACHER) {

            /*
             * Generate OTP and send it
             * to the registered email address.
             *
             * IMPORTANT:
             * JWT is NOT generated here.
             */
            otpService.generateAndSendOtp(
                    user.getEmail()
            );


            /*
             * Tell frontend that OTP
             * verification is required.
             */
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
         * ========================================================
         * STUDENT LOGIN
         * ========================================================
         *
         * Students continue using the original:
         *
         * Email + Password → JWT
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


    // ============================================================
    // VERIFY OTP
    // ============================================================

    @Override
    public LoginResponse verifyOtp(
            String email,
            String otp
    ) {

        /*
         * STEP 1
         *
         * Verify OTP:
         *
         * - OTP exists
         * - OTP is not expired
         * - OTP attempts are valid
         * - OTP hash matches
         * - OTP has not already been used
         */
        otpService.verifyOtp(
                email,
                otp
        );


        /*
         * STEP 2
         *
         * Load user.
         */
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );


        /*
         * STEP 3
         *
         * Only ADMIN and TEACHER are
         * allowed to complete OTP login.
         */
        if (user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.TEACHER) {

            throw new RuntimeException(
                    "OTP verification is available only for administrators and teachers."
            );
        }


        /*
         * STEP 4
         *
         * Check account status.
         */
        if (!Boolean.TRUE.equals(
                user.getEnabled()
        )) {

            throw new RuntimeException(
                    "Account is disabled"
            );
        }


        /*
         * STEP 5
         *
         * Build Spring Security UserDetails.
         */
        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build();


        /*
         * STEP 6
         *
         * Generate JWT ONLY AFTER
         * successful OTP verification.
         */
        String token =
                jwtService.generateToken(
                        userDetails
                );


        /*
         * STEP 7
         *
         * Return role-specific success message.
         */
        String successMessage;

        if (user.getRole() == UserRole.ADMIN) {

            successMessage =
                    "Admin login successful.";

        } else {

            successMessage =
                    "Teacher login successful.";
        }


        return LoginResponse.builder()
                .email(user.getEmail())
                .role(user.getRole().name())
                .token(token)
                .message(successMessage)
                .requiresOtp(false)
                .build();
    }


    // ============================================================
    // RESEND OTP
    // ============================================================

    @Override
    public void resendOtp(String email) {

        /*
         * OtpService handles:
         *
         * - Admin/Teacher validation
         * - 60-second resend cooldown
         * - old OTP invalidation
         * - new OTP generation
         * - OTP hashing
         * - email sending
         */
        otpService.resendOtp(email);
    }
}