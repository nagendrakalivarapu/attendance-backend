package com.smartattendance.repository;

import com.smartattendance.entity.Course;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseCode(String courseCode);

    boolean existsByCourseCode(String courseCode);

    boolean existsByCourseCodeAndIdNot(String courseCode, Long id);

    List<Course> findByDepartment(String department);

    List<Course> findBySemester(Integer semester);

    List<Course> findByTeacherId(Long teacherId);

    long countByTeacherId(Long teacherId);
}