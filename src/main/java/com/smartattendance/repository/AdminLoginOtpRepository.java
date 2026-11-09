
package com.smartattendance.repository;

import com.smartattendance.entity.AdminLoginOtp;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminLoginOtpRepository
        extends JpaRepository<AdminLoginOtp, Long> {

    Optional<AdminLoginOtp> findTopByEmailOrderByCreatedAtDesc(
            String email
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM AdminLoginOtp o
            WHERE o.email = :email
            ORDER BY o.createdAt DESC
            """)
    Optional<AdminLoginOtp> findLatestByEmailForUpdate(
            @Param("email") String email
    );

    void deleteByEmail(String email);
}
