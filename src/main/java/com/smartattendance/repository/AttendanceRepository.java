package com.smartattendance.repository;

import com.smartattendance.entity.Attendance;
import com.smartattendance.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    // ==============================
    // Attendance queries
    // ==============================

    List<Attendance> findByStudentId(Long studentId);

    List<Attendance> findByCourseId(Long courseId);

    List<Attendance> findByAttendanceDate(LocalDate date);
    // ==============================
    // Session-based attendance
    // ==============================

    boolean existsByStudentIdAndSessionId(
            Long studentId,
            Long sessionId
    );

    List<Attendance> findBySessionId(Long sessionId);

    // ==============================
    // Dashboard queries
    // ==============================

    long countByAttendanceDate(
            LocalDate date
    );

    long countByAttendanceDateAndStatus(
            LocalDate date,
            AttendanceStatus status
    );

    long countByCourseTeacherIdAndAttendanceDate(
            Long teacherId,
            LocalDate date
    );

    long countByStudentId(
            Long studentId
    );

    long countByStudentIdAndStatus(
            Long studentId,
            AttendanceStatus status
    );

    long countByStudentIdAndCourseId(
            Long studentId,
            Long courseId
    );

    long countByStudentIdAndCourseIdAndStatus(
            Long studentId,
            Long courseId,
            AttendanceStatus status
    );

    long countByCourseId(Long courseId);

    long countByCourseIdAndStatus(
            Long courseId,
            AttendanceStatus status
    );
}