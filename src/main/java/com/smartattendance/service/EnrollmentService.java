package com.smartattendance.service;

import com.smartattendance.dto.request.EnrollmentRequest;
import com.smartattendance.dto.response.EnrollmentResponse;

import java.util.List;

public interface EnrollmentService {

    EnrollmentResponse createEnrollment(
            EnrollmentRequest request
    );

    List<EnrollmentResponse> getAllEnrollments();

    EnrollmentResponse getEnrollmentById(Long id);

    List<EnrollmentResponse> getEnrollmentsByStudent(
            Long studentId
    );

    List<EnrollmentResponse> getEnrollmentsByCourse(
            Long courseId
    );

    EnrollmentResponse updateEnrollmentStatus(
            Long id,
            String status
    );

    void deleteEnrollment(Long id);
}