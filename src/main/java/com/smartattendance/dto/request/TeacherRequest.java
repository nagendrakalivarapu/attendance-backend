
package com.smartattendance.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeacherRequest {

    // User information

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 254)
    private String email;

    // Required during creation.
    // Optional during update.
    private String password;

    // Teacher information

    @NotBlank(message = "Employee ID is required")
    @Size(max = 50)
    private String employeeId;

    @NotBlank(message = "Department is required")
    @Size(max = 100)
    private String department;

    @NotBlank(message = "Designation is required")
    @Size(max = 100)
    private String designation;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20)
    private String phone;

    @NotBlank(message = "Qualification is required")
    @Size(max = 150)
    private String qualification;
}
