package com.smartattendance.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LowAttendanceResponse {

    private Long studentId;
    private String studentName;
    private String registrationNumber;

    private Long courseId;
    private String courseCode;
    private String courseName;

    private long totalSessions;
    private long presentCount;
    private long absentCount;

    private double attendancePercentage;
    private double requiredPercentage;
    private double shortagePercentage;
}