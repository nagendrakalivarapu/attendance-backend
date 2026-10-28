package com.smartattendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAttendanceDashboardResponse {

    private long totalSessions;

    private long openSessions;

    private long closedSessions;

    private long totalAttendanceRecords;

    private long presentCount;

    private long absentCount;

    private double attendancePercentage;
}