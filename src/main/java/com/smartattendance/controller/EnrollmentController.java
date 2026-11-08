package com.smartattendance.controller;

import com.smartattendance.dto.request.EnrollmentRequest;
import com.smartattendance.dto.response.EnrollmentResponse;
import com.smartattendance.service.EnrollmentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollment")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EnrollmentResponse createEnrollment(
            @Valid @RequestBody EnrollmentRequest request) {
        return enrollmentService.createEnrollment(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<EnrollmentResponse> getAllEnrollments() {
        return enrollmentService.getAllEnrollments();
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('STUDENT')")
    public List<EnrollmentResponse> getMyCourses(
            Authentication authentication) {
        return enrollmentService.getMyEnrollments(authentication.getName());
    }

    @GetMapping("/teacher/my-enrollments")
    @PreAuthorize("hasRole('TEACHER')")
    public List<EnrollmentResponse> getMyCourseEnrollments(
            Authentication authentication) {
        return enrollmentService.getTeacherEnrollments(
                authentication.getName());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public EnrollmentResponse getEnrollmentById(
            @PathVariable Long id,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return enrollmentService.getEnrollmentById(
                id, authentication.getName(), isAdmin);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<EnrollmentResponse> getByStudent(
            @PathVariable Long studentId,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return enrollmentService.getEnrollmentsByStudent(
                studentId, authentication.getName(), isAdmin);
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<EnrollmentResponse> getByCourse(
            @PathVariable Long courseId,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return enrollmentService.getEnrollmentsByCourse(
                courseId, authentication.getName(), isAdmin);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public EnrollmentResponse updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return enrollmentService.updateEnrollmentStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteEnrollment(@PathVariable Long id) {
        enrollmentService.deleteEnrollment(id);
    }
}