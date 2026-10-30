package com.smartattendance.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceEmailRequest {

    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotBlank(message = "Department is required")
    private String department;

    @NotNull(message = "Semester is required")
    private Integer semester;

    @NotBlank(message = "Section is required")
    private String section;

    @DecimalMin(
            value = "0.0",
            message = "Threshold cannot be below 0"
    )
    @DecimalMax(
            value = "100.0",
            message = "Threshold cannot exceed 100"
    )
    private double threshold = 75.0;
}