package com.smartattendance.repository;

import com.smartattendance.entity.Enrollment;
import com.smartattendance.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByCourseId(Long courseId);

    List<Enrollment> findByCourse_Teacher_Id(Long teacherId);

    List<Enrollment> findByStudentIdAndCourse_Teacher_Id(
            Long studentId,
            Long teacherId
    );

    Optional<Enrollment> findByStudentIdAndCourseId(
            Long studentId,
            Long courseId
    );

    boolean existsByStudentIdAndCourseId(
            Long studentId,
            Long courseId
    );

    long countByCourseTeacherId(Long teacherId);

    long countByStudentId(Long studentId);

    long countByCourseId(Long courseId);

    void deleteByStudentId(Long studentId);

    boolean existsByCourseId(Long courseId);

    boolean existsByStudentIdAndCourseIdAndStatus(
            Long studentId,
            Long courseId,
            EnrollmentStatus status
    );

    List<Enrollment> findByCourseIdAndStatus(
            Long courseId,
            EnrollmentStatus status
    );

    long countByCourseIdAndStatus(
            Long courseId,
            EnrollmentStatus status
    );
}