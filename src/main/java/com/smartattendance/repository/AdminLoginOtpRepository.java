package com.smartattendance.repository;

import com.smartattendance.entity.AdminLoginOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminLoginOtpRepository
        extends JpaRepository<AdminLoginOtp, Long> {

    Optional<AdminLoginOtp> findTopByEmailOrderByCreatedAtDesc(
            String email
    );

    void deleteByEmail(String email);
}