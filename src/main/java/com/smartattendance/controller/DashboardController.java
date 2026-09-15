package com.smartattendance.controller;

import com.smartattendance.dto.dashboard.AdminDashboardResponse;
import com.smartattendance.dto.dashboard.StudentDashboardResponse;
import com.smartattendance.dto.dashboard.TeacherDashboardResponse;
import com.smartattendance.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasRole('TEACHER')")
    public TeacherDashboardResponse getTeacherDashboard(
            @PathVariable Long teacherId) {

        return dashboardService.getTeacherDashboard(teacherId);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentDashboardResponse getStudentDashboard(
            @PathVariable Long studentId) {

        return dashboardService.getStudentDashboard(studentId);
    }
}