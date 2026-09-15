package com.smartattendance.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponse {

    private Long id;

    private String fullName;

    private String email;

    private String role;

    private String message;
}