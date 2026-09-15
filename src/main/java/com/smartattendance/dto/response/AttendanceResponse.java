package com.smartattendance.dto.response;

import com.smartattendance.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class AttendanceResponse {

    private Long id;

    private String studentName;

    private String courseName;

    private LocalDate attendanceDate;

    private AttendanceStatus status;

    private Long sessionId;
}