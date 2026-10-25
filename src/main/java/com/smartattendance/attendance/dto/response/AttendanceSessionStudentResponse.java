package com.smartattendance.attendance.dto.response;

import com.smartattendance.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSessionStudentResponse {

    private Long studentId;

    private String studentName;

    private String registrationNumber;

    private AttendanceStatus status;
}