package com.smartattendance.repository;

import com.smartattendance.entity.AdminLoginOtp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLoginOtpRepository
        extends JpaRepository<AdminLoginOtp, Long> {

    Optional<AdminLoginOtp> findTopByEmailOrderByCreatedAtDesc(
            String email
    );

    void deleteByEmail(String email);
}
