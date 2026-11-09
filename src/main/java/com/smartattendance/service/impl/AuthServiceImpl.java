
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
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid credentials")
                );

        // Reject disabled accounts.
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new RuntimeException("Account is disabled");
        }

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
            throw new RuntimeException("Unauthorized account role");
        }

        UserDetails userDetails = buildUserDetails(user);

        String token = jwtService.generateToken(userDetails);

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

        // Load user before attempting OTP verification.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Invalid OTP or account")
                );

        // Only ADMIN and TEACHER can use OTP login.
        if (user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.TEACHER) {
            throw new RuntimeException("Invalid OTP or account");
        }

        // Reject disabled accounts before consuming the OTP.
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account is disabled"
            );
        }

        // Verify OTP only after account checks.
        otpService.verifyOtp(email, otp);

        // Generate JWT after successful OTP verification.
        UserDetails userDetails = buildUserDetails(user);

        String token = jwtService.generateToken(userDetails);

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
        otpService.resendOtp(email);
    }

    // BUILD USER DETAILS
    private UserDetails buildUserDetails(User user) {

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
