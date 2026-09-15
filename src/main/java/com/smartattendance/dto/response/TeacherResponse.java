package com.smartattendance.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeacherResponse {

    private Long id;

    private String employeeId;
    private String department;
    private String designation;
    private String phone;
    private String qualification;

    private String fullName;
    private String email;
}