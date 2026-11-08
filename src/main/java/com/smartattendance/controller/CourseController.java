package com.smartattendance.controller;

import com.smartattendance.dto.request.CourseRequest;
import com.smartattendance.dto.response.CourseResponse;
import com.smartattendance.service.CourseService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse createCourse(
            @Valid @RequestBody CourseRequest request) {
        return courseService.createCourse(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CourseResponse> getAllCourses() {
        return courseService.getAllCourses();
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('TEACHER')")
    public List<CourseResponse> getMyCourses(
            Authentication authentication) {
        return courseService.getMyCourses(authentication.getName());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public CourseResponse getCourseById(
            @PathVariable Long id,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return courseService.getCourseById(
                id, authentication.getName(), isAdmin);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request) {
        return courseService.updateCourse(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
    }

    @GetMapping("/department/{department}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<CourseResponse> getByDepartment(
            @PathVariable String department,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return courseService.getCoursesByDepartment(
                department, authentication.getName(), isAdmin);
    }

    @GetMapping("/semester/{semester}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<CourseResponse> getBySemester(
            @PathVariable Integer semester,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return courseService.getCoursesBySemester(
                semester, authentication.getName(), isAdmin);
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CourseResponse> getByTeacher(
            @PathVariable Long teacherId) {
        return courseService.getCoursesByTeacher(teacherId);
    }
}