package com.smartattendance.controller;

import com.smartattendance.dto.dashboard.AdminDashboardResponse;
import com.smartattendance.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public AdminDashboardResponse adminDashboard() {
        return dashboardService.getAdminDashboard();
    }
}