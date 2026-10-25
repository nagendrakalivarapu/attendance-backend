package com.smartattendance.attendance.dto.response;

import com.smartattendance.enums.AttendanceSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSessionHistoryResponse {

    private Long sessionId;

    private Long courseId;

    private String courseCode;

    private String courseName;

    private LocalDate attendanceDate;

    private LocalDateTime startedAt;

    private LocalDateTime closedAt;

    private AttendanceSessionStatus status;

    private long totalStudents;

    private long presentCount;

    private long absentCount;
}