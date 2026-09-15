package com.smartattendance.controller;

import com.smartattendance.dto.response.TeacherDashboardResponse;
import com.smartattendance.service.TeacherDashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class TeacherDashboardController {

    private final TeacherDashboardService teacherDashboardService;

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherDashboardResponse>
    getTeacherDashboard() {

        return ResponseEntity.ok(
                teacherDashboardService.getTeacherDashboard()
        );
    }
}