
package com.smartattendance.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequest {

    // User information

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 254)
    private String email;

    // Required when creating a student.
    // Optional when updating an existing student.
    private String password;

    // Student information

    @NotBlank(message = "Registration number is required")
    @Size(max = 50)
    private String registrationNumber;

    @NotBlank(message = "Department is required")
    @Size(max = 100)
    private String department;

    @NotNull(message = "Semester is required")
    private Integer semester;

    @NotBlank(message = "Section is required")
    @Size(max = 20)
    private String section;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20)
    private String phone;

    private LocalDate dateOfBirth;

    private String gender;

    private String address;
}
