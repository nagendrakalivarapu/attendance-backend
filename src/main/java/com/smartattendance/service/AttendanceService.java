package com.smartattendance.service;

import com.smartattendance.dto.request.AttendanceRequest;
import com.smartattendance.dto.response.*;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    AttendanceResponse markAttendance(
            AttendanceRequest request
    );

    List<AttendanceResponse> getAttendanceByStudent(
            Long studentId
    );

    List<AttendanceResponse> getAttendanceByCourse(
            Long courseId
    );

    List<AttendanceResponse> getAttendanceByDate(
            LocalDate date
    );

    List<CourseAttendanceSummaryResponse> getCourseWiseAttendance(
            Long studentId
    );

    List<LowAttendanceResponse> getLowAttendanceStudents(
            Long courseId,
            double threshold
    );

    List<StudentCourseAttendanceResponse> getStudentCourseAttendanceReport(
            Long courseId,
            double threshold
    );

    CourseAttendanceAnalyticsResponse getCourseAttendanceAnalytics(
            Long courseId,
            double threshold
    );

    AttendanceResponse getAttendance(
            Long id
    );

    AttendanceResponse updateAttendance(
            Long id,
            AttendanceRequest request
    );

    void deleteAttendance(
            Long id
    );
    AttendanceSummaryResponse getAttendanceSummary(Long studentId);
}