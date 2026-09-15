package com.smartattendance.service;

import com.smartattendance.dto.dashboard.AdminDashboardResponse;
import com.smartattendance.dto.dashboard.StudentDashboardResponse;
import com.smartattendance.dto.dashboard.TeacherDashboardResponse;

public interface DashboardService {

    AdminDashboardResponse getAdminDashboard();

    TeacherDashboardResponse getTeacherDashboard(Long teacherId);

    StudentDashboardResponse getStudentDashboard(Long studentId);

}