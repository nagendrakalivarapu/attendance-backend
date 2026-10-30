package com.smartattendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceEmailStudentResponse {

    private Long studentId;

    private String studentName;

    private String registrationNumber;

    private String email;

    private String department;

    private Integer semester;

    private String section;

    private long totalSessions;

    private long presentCount;

    private long absentCount;

    private double attendancePercentage;

    private double requiredPercentage;

    private String status;
}