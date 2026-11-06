package com.smartattendance.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminResponse {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private Boolean enabled;
}