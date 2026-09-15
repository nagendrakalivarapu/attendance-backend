package com.smartattendance.controller;

import com.smartattendance.dto.request.EnrollmentRequest;
import com.smartattendance.dto.response.EnrollmentResponse;
import com.smartattendance.service.EnrollmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollment")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public EnrollmentResponse createEnrollment(
            @Valid @RequestBody EnrollmentRequest request) {

        return enrollmentService.createEnrollment(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<EnrollmentResponse> getAllEnrollments() {

        return enrollmentService.getAllEnrollments();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public EnrollmentResponse getEnrollmentById(
            @PathVariable Long id) {

        return enrollmentService.getEnrollmentById(id);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<EnrollmentResponse> getByStudent(
            @PathVariable Long studentId) {

        return enrollmentService
                .getEnrollmentsByStudent(studentId);
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<EnrollmentResponse> getByCourse(
            @PathVariable Long courseId) {

        return enrollmentService
                .getEnrollmentsByCourse(courseId);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public EnrollmentResponse updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        return enrollmentService
                .updateEnrollmentStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteEnrollment(
            @PathVariable Long id) {

        enrollmentService.deleteEnrollment(id);
    }
}