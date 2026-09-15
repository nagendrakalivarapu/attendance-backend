package com.smartattendance.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    private long totalStudents;

    private long totalTeachers;

    private long totalCourses;

    private long totalEnrollments;

    private long todayAttendance;

    private long presentToday;

    private long absentToday;

}