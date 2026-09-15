package com.smartattendance.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceSummaryResponse {

    private Long studentId;

    private String studentName;

    private long totalSessions;

    private long presentCount;

    private long absentCount;

    private double attendancePercentage;
}