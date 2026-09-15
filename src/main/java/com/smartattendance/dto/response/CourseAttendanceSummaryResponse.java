package com.smartattendance.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseAttendanceSummaryResponse {

    private Long courseId;

    private String courseCode;

    private String courseName;

    private long totalSessions;

    private long presentCount;

    private long absentCount;

    private double attendancePercentage;
}