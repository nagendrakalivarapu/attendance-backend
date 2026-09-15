package com.smartattendance.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardResponse {

    private String studentName;

    private long totalClasses;

    private long presentClasses;

    private long absentClasses;

    private double attendancePercentage;

}