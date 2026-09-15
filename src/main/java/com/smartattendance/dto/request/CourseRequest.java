package com.smartattendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {

    @NotBlank
    private String courseCode;

    @NotBlank
    private String courseName;

    @NotBlank
    private String department;

    @NotNull
    private Integer semester;

    @NotNull
    private Integer credits;

    @NotNull
    private Integer totalClasses;

    private Long teacherId;
}