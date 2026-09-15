package com.smartattendance.attendance.controller;

import com.smartattendance.attendance.dto.request.StartAttendanceRequest;
import com.smartattendance.attendance.dto.response.AttendanceSessionResponse;
import com.smartattendance.attendance.service.AttendanceSessionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}