
package com.smartattendance.service.impl;

import com.smartattendance.dto.auth.LoginRequest;
import com.smartattendance.dto.auth.LoginResponse;
import com.smartattendance.entity.User;
import com.smartattendance.enums.UserRole;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.security.jwt.JwtService;
import com.smartattendance.service.AuthService;
import com.smartattendance.service.OtpService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpService otpService;

    // LOGIN
    @Override
    public LoginResponse login(LoginRequest request) {

        // Authenticate email and password.
        // Disabled accounts are rejected by CustomUserDetailsService.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = findUser(request.getEmail());

        // Additional account-status verification.
        ensureAccountEnabled(user);

        // ADMIN and TEACHER require OTP.
        if (user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.TEACHER) {

            otpService.generateAndSendOtp(user.getEmail());

            return LoginResponse.builder()
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .token(null)
                    .message("OTP sent to your registered email.")
                    .requiresOtp(true)
                    .build();
        }

        // Only STUDENT accounts can log in without OTP.
        if (user.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Unauthorized account role."
            );
        }

        String token = jwtService.generateToken(
                buildUserDetails(user)
        );

        return LoginResponse.builder()
                .email(user.getEmail())
                .role(user.getRole().name())
                .token(token)
                .message("Login Successful")
                .requiresOtp(false)
                .build();
    }

    // VERIFY OTP
    @Override
    public LoginResponse verifyOtp(String email, String otp) {

        User user = findUser(email);

        // Only ADMIN and TEACHER can use OTP login.
        if (user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.TEACHER) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid OTP or account."
            );
        }

        ensureAccountEnabled(user);

        // Verify OTP before generating JWT.
        otpService.verifyOtp(user.getEmail(), otp);

        String token = jwtService.generateToken(
                buildUserDetails(user)
        );

        String successMessage =
                user.getRole() == UserRole.ADMIN
                        ? "Admin login successful."
                        : "Teacher login successful.";

        return LoginResponse.builder()
                .email(user.getEmail())
                .role(user.getRole().name())
                .token(token)
                .message(successMessage)
                .requiresOtp(false)
                .build();
    }

    // RESEND OTP
    @Override
    public void resendOtp(String email) {

        User user = findUser(email);

        // Only enabled ADMIN and TEACHER accounts can resend OTP.
        if (user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.TEACHER) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unable to process OTP request."
            );
        }

        ensureAccountEnabled(user);

        otpService.resendOtp(user.getEmail());
    }

    // FIND USER
    private User findUser(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid credentials."
                        )
                );
    }

    // CHECK ACCOUNT STATUS
    private void ensureAccountEnabled(User user) {

        if (!Boolean.TRUE.equals(user.getEnabled())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account is disabled."
            );
        }
    }

    // BUILD USER DETAILS
    private UserDetails buildUserDetails(User user) {

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                .build();
    }
}
