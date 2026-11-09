
package com.smartattendance.controller;

import com.smartattendance.dto.request.AttendanceRequest;
import com.smartattendance.dto.response.*;
import com.smartattendance.service.AttendanceService;
import com.smartattendance.service.FaceRecognitionService;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final FaceRecognitionService faceRecognitionService;

    // ============================================================
    // AUTHENTICATION HELPER
    // ============================================================

    private boolean isAdmin(Authentication authentication) {

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));
    }

    private boolean isStudent(Authentication authentication) {

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_STUDENT"));
    }

    // ============================================================
    // MANUAL ATTENDANCE
    // ============================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<AttendanceResponse> markAttendance(
            @Valid @RequestBody AttendanceRequest request,
            Authentication authentication) {

        AttendanceResponse response =
                attendanceService.markAttendance(
                        request,
                        authentication.getName(),
                        isAdmin(authentication)
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    // ============================================================
    // ATTENDANCE BY STUDENT
    // ============================================================

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByStudent(
            @PathVariable Long studentId,
            Authentication authentication) {

        return ResponseEntity.ok(
                attendanceService.getAttendanceByStudent(
                        studentId,
                        authentication.getName(),
                        isAdmin(authentication),
                        isStudent(authentication)
                )
        );
    }

    // ============================================================
    // ATTENDANCE BY COURSE
    // ============================================================

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByCourse(
            @PathVariable Long courseId,
            Authentication authentication) {

        return ResponseEntity.ok(
                attendanceService.getAttendanceByCourse(
                        courseId,
                        authentication.getName(),
                        isAdmin(authentication)
                )
        );
    }

    // ============================================================
    // ATTENDANCE BY DATE
    // ============================================================

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByDate(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Authentication authentication) {

        return ResponseEntity.ok(
                attendanceService.getAttendanceByDate(
                        date,
                        authentication.getName(),
                        isAdmin(authentication)
                )
        );
    }

    // ============================================================
    // UPDATE ATTENDANCE
    // ============================================================

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<AttendanceResponse> updateAttendance(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                attendanceService.updateAttendance(
                        id,
                        request,
                        authentication.getName(),
                        isAdmin(authentication)
                )
        );
    }

    // ============================================================
    // ADMIN DASHBOARD
    // ============================================================

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminAttendanceDashboardResponse>
    getAdminAttendanceDashboard() {

        return ResponseEntity.ok(
                attendanceService.getAdminAttendanceDashboard()
        );
    }

    // ============================================================
    // DELETE ATTENDANCE
    // ============================================================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAttendance(
            @PathVariable Long id) {

        attendanceService.deleteAttendance(id);

        return ResponseEntity.ok(
                "Attendance deleted successfully."
        );
    }

    // ============================================================
    // STUDENT PERSONAL ATTENDANCE SUMMARY
    // ============================================================

    @GetMapping("/me/summary")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AttendanceSummaryResponse>
    getMyAttendanceSummary(Authentication authentication) {

        return ResponseEntity.ok(
                attendanceService.getMyAttendanceSummary(
                        authentication.getName()
                )
        );
    }

    // ============================================================
    // STUDENT PERSONAL COURSE-WISE ATTENDANCE
    // ============================================================

    @GetMapping("/me/courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<CourseAttendanceSummaryResponse>>
    getMyCourseWiseAttendance(Authentication authentication) {

        return ResponseEntity.ok(
                attendanceService.getMyCourseWiseAttendance(
                        authentication.getName()
                )
        );
    }

    // ============================================================
    // ATTENDANCE SUMMARY BY STUDENT ID
    // ============================================================

    @GetMapping("/student/{studentId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<AttendanceSummaryResponse>
    getAttendanceSummary(
            @PathVariable Long studentId,
            Authentication authentication) {

        return ResponseEntity.ok(
                attendanceService.getAttendanceSummary(
                        studentId,
                        authentication.getName(),
                        isAdmin(authentication),
                        isStudent(authentication)
                )
        );
    }

    // ============================================================
    // COURSE-WISE ATTENDANCE BY STUDENT ID
    // ============================================================

    @GetMapping("/student/{studentId}/courses")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<List<CourseAttendanceSummaryResponse>>
    getCourseWiseAttendance(
            @PathVariable Long studentId,
            Authentication authentication) {

        return ResponseEntity.ok(
                attendanceService.getCourseWiseAttendance(
                        studentId,
                        authentication.getName(),
                        isAdmin(authentication),
                        isStudent(authentication)
                )
        );
    }

    // ============================================================
    // LOW ATTENDANCE STUDENTS
    // ============================================================

    @GetMapping("/course/{courseId}/low-attendance")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<List<LowAttendanceResponse>>
    getLowAttendanceStudents(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "75") double threshold,
            Authentication authentication) {

        return ResponseEntity.ok(
                attendanceService.getLowAttendanceStudents(
                        courseId,
                        threshold,
                        authentication.getName(),
                        isAdmin(authentication)
                )
        );
    }

    // ============================================================
    // COURSE ATTENDANCE ANALYTICS
    // ============================================================

    @GetMapping("/course/{courseId}/analytics")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<CourseAttendanceAnalyticsResponse>
    getCourseAttendanceAnalytics(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "75") double threshold,
            Authentication authentication) {

        return ResponseEntity.ok(
                attendanceService.getCourseAttendanceAnalytics(
                        courseId,
                        threshold,
                        authentication.getName(),
                        isAdmin(authentication)
                )
        );
    }

    // ============================================================
    // STUDENT COURSE ATTENDANCE REPORT
    // ============================================================

    @GetMapping("/course/{courseId}/student-report")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<List<StudentCourseAttendanceResponse>>
    getStudentCourseAttendanceReport(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "75") double threshold,
            Authentication authentication) {

        return ResponseEntity.ok(
                attendanceService.getStudentCourseAttendanceReport(
                        courseId,
                        threshold,
                        authentication.getName(),
                        isAdmin(authentication)
                )
        );
    }

    // ============================================================
    // FACE RECOGNITION ATTENDANCE
    // ============================================================

    @PostMapping(
            value = "/face/{sessionId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AttendanceResponse> markAttendanceByFace(
            @PathVariable Long sessionId,
            @RequestParam("image") MultipartFile image,
            Authentication authentication) {

        if (image == null || image.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Face image is required."
            );
        }

        String loggedInEmail = authentication.getName();

        String recognizedRegistrationNumber =
                faceRecognitionService.recognizeFace(image);

        AttendanceResponse response =
                attendanceService.markAttendanceByFace(
                        sessionId,
                        recognizedRegistrationNumber,
                        loggedInEmail
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }
}
