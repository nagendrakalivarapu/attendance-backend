
package com.smartattendance.service.impl;

import com.smartattendance.entity.AdminLoginOtp;
import com.smartattendance.entity.User;
import com.smartattendance.enums.UserRole;
import com.smartattendance.exception.OtpVerificationException;
import com.smartattendance.repository.AdminLoginOtpRepository;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.service.EmailService;
import com.smartattendance.service.OtpService;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OtpServiceImpl implements OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final AdminLoginOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    // =========================================================
    // GENERATE OTP
    // =========================================================

    @Override
    public void generateAndSendOtp(String email) {

        validateOtpUser(email);

        sendOtp(email);
    }

    // =========================================================
    // RESEND OTP
    // =========================================================

    @Override
    public void resendOtp(String email) {

        validateOtpUser(email);

        AdminLoginOtp existingOtp =
                otpRepository
                        .findTopByEmailOrderByCreatedAtDesc(email)
                        .orElse(null);

        if (existingOtp != null) {

            LocalDateTime now = LocalDateTime.now();

            LocalDateTime lastSentAt =
                    existingOtp.getLastSentAt();

            if (lastSentAt != null) {

                long secondsSinceLastSend =
                        Duration.between(
                                lastSentAt,
                                now
                        ).getSeconds();

                if (secondsSinceLastSend
                        < RESEND_COOLDOWN_SECONDS) {

                    long remaining =
                            RESEND_COOLDOWN_SECONDS
                                    - secondsSinceLastSend;

                    throw new OtpVerificationException(
                            "Please wait " + remaining
                                    + " seconds before requesting another OTP."
                    );
                }
            }
        }

        sendOtp(email);
    }

    // =========================================================
    // VERIFY OTP
    // =========================================================

    @Override
    @Transactional(noRollbackFor = OtpVerificationException.class)
    public boolean verifyOtp(String email, String otp) {

        if (email == null || email.isBlank()) {
            throw new OtpVerificationException(
                    "Email is required."
            );
        }

        if (otp == null || !otp.matches("\\d{6}")) {
            throw new OtpVerificationException(
                    "Invalid OTP."
            );
        }

        // Fetch OTP with a pessimistic database lock.
        AdminLoginOtp otpRecord =
                otpRepository
                        .findLatestByEmailForUpdate(email)
                        .orElseThrow(() ->
                                new OtpVerificationException(
                                        "OTP not found. Please request a new OTP."
                                )
                        );

        LocalDateTime now = LocalDateTime.now();

        // Prevent OTP reuse.
        if (Boolean.TRUE.equals(otpRecord.getVerified())) {

            throw new OtpVerificationException(
                    "OTP has already been used."
            );
        }

        // Check expiry.
        if (otpRecord.getExpiresAt() == null
                || !otpRecord.getExpiresAt().isAfter(now)) {

            throw new OtpVerificationException(
                    "OTP has expired. Please request a new OTP."
            );
        }

        int attempts = otpRecord.getAttempts() == null
                ? 0
                : otpRecord.getAttempts();

        // Block after maximum attempts.
        if (attempts >= MAX_ATTEMPTS) {

            throw new OtpVerificationException(
                    "Maximum OTP attempts exceeded. Please request a new OTP."
            );
        }

        // Compare submitted OTP with stored password hash.
        if (!passwordEncoder.matches(
                otp,
                otpRecord.getOtpHash()
        )) {

            // Increment and persist failed attempt.
            attempts++;

            otpRecord.setAttempts(attempts);

            otpRepository.saveAndFlush(otpRecord);

            int remaining = MAX_ATTEMPTS - attempts;

            if (remaining <= 0) {

                throw new OtpVerificationException(
                        "Maximum OTP attempts exceeded. Please request a new OTP."
                );
            }

            throw new OtpVerificationException(
                    "Invalid OTP. "
                            + remaining
                            + " attempts remaining."
            );
        }

        // Mark OTP as used.
        otpRecord.setVerified(true);

        otpRepository.saveAndFlush(otpRecord);

        return true;
    }

    // =========================================================
    // SEND OTP
    // =========================================================

    private void sendOtp(String email) {

        // Remove previous OTP records.
        otpRepository.deleteByEmail(email);

        String otp = generateOtp();

        LocalDateTime now = LocalDateTime.now();

        AdminLoginOtp otpRecord =
                AdminLoginOtp.builder()
                        .email(email)
                        .otpHash(
                                passwordEncoder.encode(otp)
                        )
                        .expiresAt(
                                now.plusMinutes(
                                        OTP_EXPIRY_MINUTES
                                )
                        )
                        .attempts(0)
                        .verified(false)
                        .lastSentAt(now)
                        .build();

        otpRepository.saveAndFlush(otpRecord);

        // Send OTP to registered email.
        emailService.sendAdminLoginOtp(
                email,
                otp
        );
    }

    // =========================================================
    // GENERATE SECURE SIX-DIGIT OTP
    // =========================================================

    private String generateOtp() {

        int value = secureRandom.nextInt(1_000_000);

        return String.format("%06d", value);
    }

    // =========================================================
    // VALIDATE USER
    // =========================================================

    private void validateOtpUser(String email) {

        if (email == null || email.isBlank()) {

            throw new OtpVerificationException(
                    "Email is required."
            );
        }

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new OtpVerificationException(
                                        "User account not found."
                                )
                        );

        if (user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.TEACHER) {

            throw new OtpVerificationException(
                    "OTP login is available only for administrators and teachers."
            );
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {

            throw new OtpVerificationException(
                    "Account is disabled."
            );
        }
    }
}
