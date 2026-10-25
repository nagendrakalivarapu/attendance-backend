package com.smartattendance.attendance.service;

import java.util.List;
import com.smartattendance.attendance.dto.request.StartAttendanceRequest;
import com.smartattendance.attendance.dto.response.AttendanceSessionResponse;

public interface AttendanceSessionService {

    AttendanceSessionResponse startSession(
            StartAttendanceRequest request
    );

    AttendanceSessionResponse getSession(Long id);

    AttendanceSessionResponse getOpenSessionForCourse(
            Long courseId
    );
    List<AttendanceSessionResponse> getMyOpenSessions(String email);

    void closeSession(Long id);

}