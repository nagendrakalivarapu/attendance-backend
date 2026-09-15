package com.smartattendance.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseAttendanceAnalyticsResponse {

    private Long courseId;

    private String courseCode;

    private String courseName;

    private long totalStudents;

    private long totalSessions;

    private long totalPresent;

    private long totalAbsent;

    private double averageAttendancePercentage;

    private long lowAttendanceStudents;

    private double requiredPercentage;
}