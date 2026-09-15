package com.smartattendance.dto.request;

import lombok.Data;

@Data
public class TeacherRequest {

    private String employeeId;
    private String department;
    private String designation;
    private String phone;
    private String qualification;
    private Long userId;
}