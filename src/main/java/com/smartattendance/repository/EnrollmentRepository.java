package com.smartattendance.repository;

import com.smartattendance.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByCourseId(Long courseId);

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
}