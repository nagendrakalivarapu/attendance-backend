package com.smartattendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceEmailPreviewResponse {

    private Long courseId;

    private String courseCode;

    private String courseName;

    private String department;

    private Integer semester;

    private String section;

    private double requiredPercentage;

    private long totalStudents;

    private long goodAttendanceStudents;

    private long lowAttendanceStudents;

    private List<AttendanceEmailStudentResponse> students;
}