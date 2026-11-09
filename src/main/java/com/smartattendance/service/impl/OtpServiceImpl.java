
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
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OtpServiceImpl implements OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_RESENDS = 3;

    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 15;

    private final AdminLoginOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    // =====================================================
    // GENERATE OTP
    // =====================================================

    @Override
    public void generateAndSendOtp(String email) {

        email = normalizeEmail(email);

        validateOtpUser(email);

        issueOtp(email);
    }

    // =====================================================
    // RESEND OTP
    // =====================================================

    @Override
    public void resendOtp(String email) {

        email = normalizeEmail(email);

        validateOtpUser(email);

        AdminLoginOtp record = otpRepository
                .findByEmailForUpdate(email)
                .orElseThrow(() ->
                        new OtpVerificationException(
                                "OTP not found. Please login again."
                        )
                );

        LocalDateTime now = LocalDateTime.now();

        if (Boolean.TRUE.equals(record.getVerified())) {
            throw new OtpVerificationException(
                    "OTP has already been used. Please login again."
            );
        }

        refreshRateLimitWindows(record, now);

        checkVerificationAttempts(record);

        checkResendCooldown(record, now);

        checkResendLimit(record);

        record.setResendCount(record.getResendCount() + 1);

        sendNewOtp(record, now);
    }

    // =====================================================
    // VERIFY OTP
    // =====================================================

    @Override
    @Transactional(noRollbackFor = OtpVerificationException.class)
    public boolean verifyOtp(String email, String otp) {

        email = normalizeEmail(email);

        if (otp == null || !otp.matches("\\d{6}")) {
            throw new OtpVerificationException("Invalid OTP.");
        }

        AdminLoginOtp record = otpRepository
                .findByEmailForUpdate(email)
                .orElseThrow(() ->
                        new OtpVerificationException(
                                "OTP not found. Please request a new OTP."
                        )
                );

        LocalDateTime now = LocalDateTime.now();

        if (Boolean.TRUE.equals(record.getVerified())) {
            throw new OtpVerificationException(
                    "OTP has already been used."
            );
        }

        if (record.getExpiresAt() == null
                || !record.getExpiresAt().isAfter(now)) {

            throw new OtpVerificationException(
                    "OTP has expired. Please request a new OTP."
            );
        }

        refreshRateLimitWindows(record, now);

        checkVerificationAttempts(record);

        if (!passwordEncoder.matches(
                otp,
                record.getOtpHash()
        )) {

            record.setAttempts(record.getAttempts() + 1);

            otpRepository.saveAndFlush(record);

            int remaining =
                    MAX_ATTEMPTS - record.getAttempts();

            throw new OtpVerificationException(
                    remaining > 0
                            ? "Invalid OTP. " + remaining
                                + " attempts remaining."
                            : "Maximum OTP attempts exceeded."
            );
        }

        // Mark OTP as used.
        record.setVerified(true);

        otpRepository.saveAndFlush(record);

        return true;
    }

    // =====================================================
    // ISSUE OTP
    // =====================================================

    private void issueOtp(String email) {

        LocalDateTime now = LocalDateTime.now();

        AdminLoginOtp record = otpRepository
                .findByEmailForUpdate(email)
                .orElse(null);

        if (record == null) {

            record = AdminLoginOtp.builder()
                    .email(email)
                    .otpHash("")
                    .expiresAt(now)
                    .attempts(0)
                    .verified(false)
                    .lastSentAt(now)
                    .resendCount(0)
                    .resendWindowStartedAt(now)
                    .attemptWindowStartedAt(now)
                    .build();

            sendNewOtp(record, now);

            return;
        }

        if (Boolean.TRUE.equals(record.getVerified())) {

            // A new authenticated login starts a new challenge.
            record.setAttempts(0);
            record.setResendCount(0);
            record.setAttemptWindowStartedAt(now);
            record.setResendWindowStartedAt(now);

            sendNewOtp(record, now);

            return;
        }

        refreshRateLimitWindows(record, now);

        checkVerificationAttempts(record);

        checkResendCooldown(record, now);

        checkResendLimit(record);

        record.setResendCount(record.getResendCount() + 1);

        sendNewOtp(record, now);
    }

    // =====================================================
    // SEND NEW OTP
    // =====================================================

    private void sendNewOtp(
            AdminLoginOtp record,
            LocalDateTime now
    ) {

        String otp = generateOtp();

        record.setOtpHash(
                passwordEncoder.encode(otp)
        );

        record.setExpiresAt(
                now.plusMinutes(OTP_EXPIRY_MINUTES)
        );

        record.setLastSentAt(now);

        if (record.getAttempts() == null) {
            record.setAttempts(0);
        }

        if (record.getResendCount() == null) {
            record.setResendCount(0);
        }

        if (record.getAttemptWindowStartedAt() == null) {
            record.setAttemptWindowStartedAt(now);
        }

        if (record.getResendWindowStartedAt() == null) {
            record.setResendWindowStartedAt(now);
        }

        record.setVerified(false);

        // Save before email delivery.
        otpRepository.saveAndFlush(record);

        emailService.sendAdminLoginOtp(
                record.getEmail(),
                otp
        );
    }

    // =====================================================
    // RATE LIMIT WINDOW MANAGEMENT
    // =====================================================

    private void refreshRateLimitWindows(
            AdminLoginOtp record,
            LocalDateTime now
    ) {

        LocalDateTime attemptWindow =
                record.getAttemptWindowStartedAt();

        if (attemptWindow == null
                || !attemptWindow.plusMinutes(
                        RATE_LIMIT_WINDOW_MINUTES
                ).isAfter(now)) {

            record.setAttempts(0);
            record.setAttemptWindowStartedAt(now);
        }

        LocalDateTime resendWindow =
                record.getResendWindowStartedAt();

        if (resendWindow == null
                || !resendWindow.plusMinutes(
                        RATE_LIMIT_WINDOW_MINUTES
                ).isAfter(now)) {

            record.setResendCount(0);
            record.setResendWindowStartedAt(now);
        }
    }

    // =====================================================
    // VERIFICATION ATTEMPT CHECK
    // =====================================================

    private void checkVerificationAttempts(
            AdminLoginOtp record
    ) {

        if (record.getAttempts() >= MAX_ATTEMPTS) {

            throw new OtpVerificationException(
                    "Maximum OTP attempts exceeded. Please try again later."
            );
        }
    }

    // =====================================================
    // RESEND COOLDOWN CHECK
    // =====================================================

    private void checkResendCooldown(
            AdminLoginOtp record,
            LocalDateTime now
    ) {

        if (record.getLastSentAt() == null) {
            return;
        }

        long elapsed = Duration.between(
                record.getLastSentAt(),
                now
        ).getSeconds();

        if (elapsed < RESEND_COOLDOWN_SECONDS) {

            long remaining =
                    RESEND_COOLDOWN_SECONDS - elapsed;

            throw new OtpVerificationException(
                    "Please wait " + remaining
                            + " seconds before requesting another OTP."
            );
        }
    }

    // =====================================================
    // RESEND LIMIT CHECK
    // =====================================================

    private void checkResendLimit(
            AdminLoginOtp record
    ) {

        if (record.getResendCount() >= MAX_RESENDS) {

            throw new OtpVerificationException(
                    "Maximum OTP resend limit reached. Please try again later."
            );
        }
    }

    // =====================================================
    // GENERATE SECURE OTP
    // =====================================================

    private String generateOtp() {

        return String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(1_000_000)
        );
    }

    // =====================================================
    // NORMALIZE EMAIL
    // =====================================================

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {

            throw new OtpVerificationException(
                    "Email is required."
            );
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    // =====================================================
    // VALIDATE OTP USER
    // =====================================================

    private void validateOtpUser(String email) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new OtpVerificationException(
                                "Unable to process OTP request."
                        )
                );

        if (user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.TEACHER) {

            throw new OtpVerificationException(
                    "Unable to process OTP request."
            );
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {

            throw new OtpVerificationException(
                    "Account is disabled."
            );
        }
    }
}
