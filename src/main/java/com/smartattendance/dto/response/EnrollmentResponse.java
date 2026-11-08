package com.smartattendance.dto.response;

import com.smartattendance.enums.EnrollmentStatus;
import java.time.LocalDate;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private Long id;

    private Long studentId;

    private String studentName;

    private String registrationNumber;

    private Long courseId;

    private String courseCode;

    private String courseName;

    private LocalDate enrollmentDate;

    private EnrollmentStatus status;
}
