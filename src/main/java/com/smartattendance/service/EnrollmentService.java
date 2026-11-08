package com.smartattendance.service;

import com.smartattendance.dto.request.EnrollmentRequest;
import com.smartattendance.dto.response.EnrollmentResponse;
import java.util.List;

public interface EnrollmentService {

    EnrollmentResponse createEnrollment(EnrollmentRequest request);

    List<EnrollmentResponse> getAllEnrollments();

    EnrollmentResponse getEnrollmentById(
            Long id, String email, boolean isAdmin);

    List<EnrollmentResponse> getEnrollmentsByStudent(
            Long studentId, String email, boolean isAdmin);

    List<EnrollmentResponse> getMyEnrollments(String email);

    List<EnrollmentResponse> getTeacherEnrollments(String email);

    List<EnrollmentResponse> getEnrollmentsByCourse(
            Long courseId, String email, boolean isAdmin);

    EnrollmentResponse updateEnrollmentStatus(Long id, String status);

    void deleteEnrollment(Long id);
}