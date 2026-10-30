package com.smartattendance.attendance.controller;

import com.smartattendance.attendance.dto.request.StartAttendanceRequest;
import com.smartattendance.attendance.dto.response.AttendanceSessionHistoryResponse;
import com.smartattendance.attendance.dto.response.AttendanceSessionResponse;
import com.smartattendance.attendance.dto.response.AttendanceSessionStudentResponse;
import com.smartattendance.attendance.service.AttendanceSessionService;
import java.util.List;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance-sessions")
@RequiredArgsConstructor
public class AttendanceSessionController {

    private final AttendanceSessionService attendanceSessionService;


    // ============================================================
    // START ATTENDANCE SESSION
    // ============================================================

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AttendanceSessionResponse> startSession(
            @Valid @RequestBody StartAttendanceRequest request) {

        AttendanceSessionResponse response =
                attendanceSessionService.startSession(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ============================================================
    // GET OPEN ATTENDANCE SESSION FOR COURSE
    // ============================================================

    @GetMapping("/course/{courseId}/open")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AttendanceSessionResponse> getOpenSession(
            @PathVariable Long courseId) {

        return ResponseEntity.ok(
                attendanceSessionService
                        .getOpenSessionForCourse(courseId)
        );
    }

    // ============================================================
    // GET ATTENDANCE SESSION
    // ============================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<AttendanceSessionResponse> getSession(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                attendanceSessionService.getSession(id)
        );
    }


    // ============================================================
    // CLOSE ATTENDANCE SESSION
    // ============================================================

    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<String> closeSession(
            @PathVariable Long id) {

        attendanceSessionService.closeSession(id);

        return ResponseEntity.ok(
                "Attendance session closed successfully."
        );
    }

    @GetMapping("/my-open")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AttendanceSessionResponse>> getMyOpenSessions(
            Authentication authentication) {

        return ResponseEntity.ok(
                attendanceSessionService.getMyOpenSessions(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AttendanceSessionHistoryResponse>> getMyHistory(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                attendanceSessionService.getMyHistory(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/{sessionId}/attendance")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AttendanceSessionStudentResponse>>
    getSessionAttendance(
            @PathVariable Long sessionId,
            Authentication authentication) {

        return ResponseEntity.ok(
                attendanceSessionService.getSessionAttendance(
                        sessionId,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/admin/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AttendanceSessionHistoryResponse>>
    getAdminHistory() {

        return ResponseEntity.ok(
                attendanceSessionService.getAdminHistory()
        );
    }
}