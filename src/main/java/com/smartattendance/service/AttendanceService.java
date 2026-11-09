
package com.smartattendance.service;

import com.smartattendance.dto.request.AttendanceRequest;
import com.smartattendance.dto.response.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface AttendanceService {

    AttendanceResponse markAttendance(
            AttendanceRequest request,
            String email,
            boolean isAdmin
    );

    AttendanceResponse markAttendanceByFace(
            Long sessionId,
            String recognizedRegistrationNumber,
            String loggedInEmail
    );

    List<AttendanceResponse> getAttendanceByStudent(
            Long studentId,
            String email,
            boolean isAdmin,
            boolean isStudent
    );

    AttendanceSummaryResponse getAttendanceSummary(
            Long studentId,
            String email,
            boolean isAdmin,
            boolean isStudent
    );

    List<CourseAttendanceSummaryResponse> getCourseWiseAttendance(
            Long studentId,
            String email,
            boolean isAdmin,
            boolean isStudent
    );

    AttendanceSummaryResponse getMyAttendanceSummary(String email);

    List<CourseAttendanceSummaryResponse> getMyCourseWiseAttendance(
            String email
    );

    List<AttendanceResponse> getAttendanceByCourse(
            Long courseId,
            String email,
            boolean isAdmin
    );

    List<AttendanceResponse> getAttendanceByDate(
            LocalDate date,
            String email,
            boolean isAdmin
    );

    List<LowAttendanceResponse> getLowAttendanceStudents(
            Long courseId,
            double threshold,
            String email,
            boolean isAdmin
    );

    List<StudentCourseAttendanceResponse> getStudentCourseAttendanceReport(
            Long courseId,
            double threshold,
            String email,
            boolean isAdmin
    );

    CourseAttendanceAnalyticsResponse getCourseAttendanceAnalytics(
            Long courseId,
            double threshold,
            String email,
            boolean isAdmin
    );

    AdminAttendanceDashboardResponse getAdminAttendanceDashboard();

    AttendanceResponse getAttendance(Long id);

    AttendanceResponse updateAttendance(
            Long id,
            AttendanceRequest request,
            String email,
            boolean isAdmin
    );

    void deleteAttendance(Long id);

    boolean registerFace(Long studentId, MultipartFile image);
}
