package com.smartattendance.dto.response;

import com.smartattendance.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private UserRole role;
    private Boolean enabled;
}