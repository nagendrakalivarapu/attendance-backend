
package com.smartattendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "admin_login_otps",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_admin_login_otp_email",
                        columnNames = "email"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLoginOtp extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String otpHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private Integer attempts = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean verified = false;

    @Column(nullable = false)
    private LocalDateTime lastSentAt;

    // Number of OTP resends in the current window.
    @Builder.Default
    @Column(nullable = false)
    private Integer resendCount = 0;

    // Start of the resend rate-limit window.
    @Column
    private LocalDateTime resendWindowStartedAt;

    // Start of the verification-attempt window.
    @Column
    private LocalDateTime attemptWindowStartedAt;
}
