
package com.smartattendance.repository;

import com.smartattendance.entity.AdminLoginOtp;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminLoginOtpRepository
        extends JpaRepository<AdminLoginOtp, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM AdminLoginOtp o
            WHERE o.email = :email
            """)
    Optional<AdminLoginOtp> findByEmailForUpdate(
            @Param("email") String email
    );

}
