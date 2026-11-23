package com.smartattendance.controller;

import com.smartattendance.dto.dashboard.AdminDashboardResponse;
import com.smartattendance.dto.dashboard.StudentDashboardResponse;
import com.smartattendance.dto.dashboard.TeacherDashboardResponse;
import com.smartattendance.service.DashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboardResponse getAdminDashboard() {
        return dashboardService.getAdminDashboard();
    }

    @GetMapping("/teacher/me")
    @PreAuthorize("hasRole('TEACHER')")
    public TeacherDashboardResponse getTeacherDashboard(
            Authentication authentication) {

        return dashboardService.getTeacherDashboard(
                authentication.getName()
        );
    }

    @GetMapping("/student/me")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentDashboardResponse getStudentDashboard(
            Authentication authentication) {

        return dashboardService.getStudentDashboard(
                authentication.getName()
        );
    }
}
