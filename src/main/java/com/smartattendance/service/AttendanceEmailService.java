package com.smartattendance.service;

import com.smartattendance.dto.request.AttendanceEmailRequest;
import com.smartattendance.dto.response.AttendanceEmailPreviewResponse;
import com.smartattendance.dto.response.AttendanceEmailSendResponse;

public interface AttendanceEmailService {

    AttendanceEmailPreviewResponse previewAttendanceEmails(
            AttendanceEmailRequest request
    );

    AttendanceEmailSendResponse sendAttendanceEmails(
            AttendanceEmailRequest request
    );
}