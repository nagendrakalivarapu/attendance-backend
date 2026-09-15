package com.smartattendance.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private Long id;

    private String courseCode;

    private String courseName;

    private String department;

    private Integer semester;

    private Integer credits;

    private Integer totalClasses;

    private Long teacherId;

    private String teacherName;
}