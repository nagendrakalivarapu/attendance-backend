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
public class AttendanceEmailSendResponse {

    private long totalStudents;

    private long sentCount;

    private long failedCount;

    private List<String> sentEmails;

    private List<String> failedEmails;

    private String message;
}