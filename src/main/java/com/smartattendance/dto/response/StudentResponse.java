package com.smartattendance.dto.response;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {

    private Long id;

    private String registrationNumber;

    private String department;

    private Integer semester;

    private String section;

    private String phone;

    private LocalDate dateOfBirth;

    private String gender;

    private String address;

    private String studentName;

    private String email;

    private Boolean faceRegistered;

    private String profilePicture;
}
