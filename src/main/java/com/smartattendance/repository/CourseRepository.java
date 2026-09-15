package com.smartattendance.repository;

import com.smartattendance.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository
        extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseCode(String courseCode);

    boolean existsByCourseCode(String courseCode);

    List<Course> findByDepartment(String department);

    List<Course> findBySemester(Integer semester);

    List<Course> findByTeacherId(Long teacherId);

    long countByTeacherId(Long teacherId);
}