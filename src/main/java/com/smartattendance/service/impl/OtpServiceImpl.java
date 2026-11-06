package com.smartattendance.service.impl;

import com.smartattendance.entity.AdminLoginOtp;
import com.smartattendance.entity.User;
import com.smartattendance.enums.UserRole;
import com.smartattendance.repository.AdminLoginOtpRepository;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.service.EmailService;
import com.smartattendance.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

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

    private final SecureRandom secureRandom =
            new SecureRandom();

    @Override
    public void generateAndSendOtp(String email) {

        validateOtpUser(email);

        sendOtp(email);
    }

    @Override
    public void resendOtp(String email) {

        validateOtpUser(email);

        AdminLoginOtp existingOtp =
                otpRepository
                        .findTopByEmailOrderByCreatedAtDesc(email)
                        .orElse(null);

        if (existingOtp != null) {

            LocalDateTime now =
                    LocalDateTime.now();

            long secondsSinceLastSend =
                    Duration.between(
                            existingOtp.getLastSentAt(),
                            now
                    ).getSeconds();

            if (secondsSinceLastSend
                    < RESEND_COOLDOWN_SECONDS) {

                long remaining =
                        RESEND_COOLDOWN_SECONDS
                                - secondsSinceLastSend;

                throw new RuntimeException(
                        "Please wait "
                                + remaining
                                + " seconds before requesting another OTP."
                );
            }
        }

        sendOtp(email);
    }

    @Override
    public boolean verifyOtp(
            String email,
            String otp
    ) {

        if (email == null || email.isBlank()) {
            throw new RuntimeException(
                    "Email is required."
            );
        }

        if (otp == null || otp.isBlank()) {
            throw new RuntimeException(
                    "OTP is required."
            );
        }

        AdminLoginOtp otpRecord =
                otpRepository
                        .findTopByEmailOrderByCreatedAtDesc(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "OTP not found. Please request a new OTP."
                                )
                        );

        if (Boolean.TRUE.equals(
                otpRecord.getVerified())) {

            throw new RuntimeException(
                    "OTP has already been used."
            );
        }

        if (otpRecord.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "OTP has expired. Please request a new OTP."
            );
        }

        if (otpRecord.getAttempts()
                >= MAX_ATTEMPTS) {

            throw new RuntimeException(
                    "Maximum OTP attempts exceeded. Please request a new OTP."
            );
        }

        if (!passwordEncoder.matches(
                otp,
                otpRecord.getOtpHash()
        )) {

            otpRecord.setAttempts(
                    otpRecord.getAttempts() + 1
            );

            otpRepository.save(otpRecord);

            int remaining =
                    MAX_ATTEMPTS
                            - otpRecord.getAttempts();

            if (remaining <= 0) {
                throw new RuntimeException(
                        "Maximum OTP attempts exceeded. Please request a new OTP."
                );
            }

            throw new RuntimeException(
                    "Invalid OTP. "
                            + remaining
                            + " attempts remaining."
            );
        }

        otpRecord.setVerified(true);

        otpRepository.save(otpRecord);

        return true;
    }

    private void sendOtp(String email) {

        otpRepository.deleteByEmail(email);

        String otp = generateOtp();

        LocalDateTime now =
                LocalDateTime.now();

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

        otpRepository.save(otpRecord);

        emailService.sendAdminLoginOtp(
                email,
                otp
        );
    }

    private String generateOtp() {

        int value =
                secureRandom.nextInt(1_000_000);

        return String.format(
                "%06d",
                value
        );
    }

    private void validateOtpUser(String email) {

        if (email == null || email.isBlank()) {
            throw new RuntimeException(
                    "Email is required."
            );
        }

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User account not found."
                                )
                        );

        if (user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.TEACHER) {

            throw new RuntimeException(
                    "OTP login is available only for administrators and teachers."
            );
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {

            throw new RuntimeException(
                    "Account is disabled."
            );
        }
    }

}