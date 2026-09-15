package com.smartattendance.controller;

import com.smartattendance.dto.request.CourseRequest;
import com.smartattendance.dto.response.CourseResponse;
import com.smartattendance.service.CourseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse createCourse(
            @Valid @RequestBody CourseRequest request) {

        return courseService.createCourse(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<CourseResponse> getAllCourses() {

        return courseService.getAllCourses();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public CourseResponse getCourseById(
            @PathVariable Long id) {

        return courseService.getCourseById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request) {

        return courseService.updateCourse(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCourse(
            @PathVariable Long id) {

        courseService.deleteCourse(id);
    }

    @GetMapping("/department/{department}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<CourseResponse> getByDepartment(
            @PathVariable String department) {

        return courseService
                .getCoursesByDepartment(department);
    }

    @GetMapping("/semester/{semester}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<CourseResponse> getBySemester(
            @PathVariable Integer semester) {

        return courseService
                .getCoursesBySemester(semester);
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<CourseResponse> getByTeacher(
            @PathVariable Long teacherId) {

        return courseService
                .getCoursesByTeacher(teacherId);
    }
}