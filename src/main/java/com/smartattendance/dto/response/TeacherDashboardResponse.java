package com.smartattendance.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeacherDashboardResponse {

    private Long teacherId;

    private String teacherName;

    private long totalCourses;

    private long totalStudents;

    private long totalSessions;

    private double averageAttendancePercentage;

    private long lowAttendanceStudents;

    private List<TeacherCourseSummaryResponse> courses;
}
