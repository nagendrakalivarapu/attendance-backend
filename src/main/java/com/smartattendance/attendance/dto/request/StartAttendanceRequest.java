package com.smartattendance.attendance.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartAttendanceRequest {

    @NotNull
    private Long courseId;
}