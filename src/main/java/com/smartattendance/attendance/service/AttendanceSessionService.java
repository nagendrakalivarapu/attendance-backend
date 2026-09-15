package com.smartattendance.attendance.service;

import com.smartattendance.attendance.dto.request.StartAttendanceRequest;
import com.smartattendance.attendance.dto.response.AttendanceSessionResponse;

public interface AttendanceSessionService {

    AttendanceSessionResponse startSession(
            StartAttendanceRequest request
    );

    AttendanceSessionResponse getSession(Long id);

    void closeSession(Long id);
}