package com.smartattendance.controller;

import com.smartattendance.dto.request.AttendanceEmailRequest;
import com.smartattendance.dto.response.AttendanceEmailPreviewResponse;
import com.smartattendance.dto.response.AttendanceEmailSendResponse;
import com.smartattendance.service.AttendanceEmailService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance-email")
@RequiredArgsConstructor
public class AttendanceEmailController {

    private final AttendanceEmailService attendanceEmailService;


    // ============================================================
    // PREVIEW
    // ============================================================

    @PostMapping("/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttendanceEmailPreviewResponse>
    preview(
            @Valid
            @RequestBody
            AttendanceEmailRequest request) {

        return ResponseEntity.ok(
                attendanceEmailService
                        .previewAttendanceEmails(request)
        );
    }


    // ============================================================
    // SEND
    // ============================================================

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttendanceEmailSendResponse>
    send(
            @Valid
            @RequestBody
            AttendanceEmailRequest request) {

        return ResponseEntity.ok(
                attendanceEmailService
                        .sendAttendanceEmails(request)
        );
    }
}