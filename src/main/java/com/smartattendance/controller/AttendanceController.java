package com.smartattendance.controller;

import com.smartattendance.dto.request.AttendanceRequest;
import com.smartattendance.dto.response.*;
import com.smartattendance.service.AttendanceService;
import com.smartattendance.service.FaceRecognitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final FaceRecognitionService faceRecognitionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<AttendanceResponse> markAttendance(
            @Valid @RequestBody AttendanceRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.markAttendance(request));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByStudent(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(
                attendanceService.getAttendanceByStudent(studentId));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByCourse(
            @PathVariable Long courseId) {

        return ResponseEntity.ok(
                attendanceService.getAttendanceByCourse(courseId));
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByDate(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return ResponseEntity.ok(
                attendanceService.getAttendanceByDate(date));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ResponseEntity<AttendanceResponse> updateAttendance(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceRequest request) {

        return ResponseEntity.ok(
                attendanceService.updateAttendance(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteAttendance(
            @PathVariable Long id) {

        attendanceService.deleteAttendance(id);

        return ResponseEntity.ok("Attendance deleted successfully.");
    }

    @GetMapping("/student/{studentId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<AttendanceSummaryResponse> getAttendanceSummary(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(
                attendanceService.getAttendanceSummary(studentId)
        );
    }

    @GetMapping("/student/{studentId}/courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<CourseAttendanceSummaryResponse>>
    getCourseWiseAttendance(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(
                attendanceService.getCourseWiseAttendance(studentId)
        );
    }

    @GetMapping("/course/{courseId}/low-attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<LowAttendanceResponse>>
    getLowAttendanceStudents(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "75") double threshold) {

        return ResponseEntity.ok(
                attendanceService.getLowAttendanceStudents(
                        courseId,
                        threshold
                )
        );
    }

    @GetMapping("/course/{courseId}/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<CourseAttendanceAnalyticsResponse>
    getCourseAttendanceAnalytics(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "75") double threshold) {

        return ResponseEntity.ok(
                attendanceService.getCourseAttendanceAnalytics(
                        courseId,
                        threshold
                )
        );
    }

    @GetMapping("/course/{courseId}/student-report")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<StudentCourseAttendanceResponse>>
    getStudentCourseAttendanceReport(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "75") double threshold) {

        return ResponseEntity.ok(
                attendanceService.getStudentCourseAttendanceReport(
                        courseId,
                        threshold
                )
        );
    }

    @PostMapping(
            value = "/face/{sessionId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AttendanceResponse> markAttendanceByFace(
            @PathVariable Long sessionId,
            @RequestParam("image") MultipartFile image) {

        if (image == null || image.isEmpty()) {

            throw new RuntimeException(
                    "Face image is required."
            );
        }

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String loggedInEmail =
                authentication.getName();

        String recognizedRegistrationNumber =
                faceRecognitionService.recognizeFace(image);

        AttendanceResponse response =
                attendanceService.markAttendanceByFace(
                        sessionId,
                        recognizedRegistrationNumber,
                        loggedInEmail
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}