package com.smartattendance.dto.request;

import lombok.Data;

@Data
public class TeacherRequest {

    private String fullName;
    private String email;
    private String password;

    private String employeeId;
    private String department;
    private String designation;
    private String phone;
    private String qualification;
}