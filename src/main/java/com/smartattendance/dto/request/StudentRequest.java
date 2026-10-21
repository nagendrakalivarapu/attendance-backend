package com.smartattendance.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequest {

    // User information
    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    private String password;

    // Student information
    @NotBlank
    private String registrationNumber;

    @NotBlank
    private String department;

    @NotNull
    private Integer semester;

    @NotBlank
    private String section;

    @NotBlank
    private String phone;

    private LocalDate dateOfBirth;

    private String gender;

    private String address;

    // Used only internally for existing students/update
    private Long userId;
}