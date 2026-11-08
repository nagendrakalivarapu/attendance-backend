package com.smartattendance.repository;

import com.smartattendance.entity.Enrollment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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
}