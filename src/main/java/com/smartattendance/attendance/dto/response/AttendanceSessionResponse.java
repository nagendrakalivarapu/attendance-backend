package com.smartattendance.attendance.dto.response;

import lombok.Builder;
import lombok.Getter;
import com.smartattendance.enums.AttendanceSessionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AttendanceSessionResponse {

    private Long id;

    private Long courseId;

    private Long teacherId;

    private LocalDate attendanceDate;

    private LocalDateTime startedAt;

    private LocalDateTime closedAt;

    private AttendanceSessionStatus status;

    private Long sessionId;
}