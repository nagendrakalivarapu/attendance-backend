package com.smartattendance.attendance.repository;

import com.smartattendance.attendance.entity.AttendanceSession;
import com.smartattendance.enums.AttendanceSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository
        extends JpaRepository<AttendanceSession, Long> {

    List<AttendanceSession> findByStatus(
            AttendanceSessionStatus status
    );

    Optional<AttendanceSession> findByIdAndStatus(
            Long id,
            AttendanceSessionStatus status
    );
    boolean existsByCourseIdAndStatus(
            Long courseId,
            AttendanceSessionStatus status
    );

    long countByCourseIdAndStatus(
            Long courseId,
            AttendanceSessionStatus status
    );


}