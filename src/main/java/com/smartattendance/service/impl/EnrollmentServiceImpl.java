package com.smartattendance.service.impl;

import com.smartattendance.dto.request.EnrollmentRequest;
import com.smartattendance.dto.response.EnrollmentResponse;
import com.smartattendance.entity.Course;
import com.smartattendance.entity.Enrollment;
import com.smartattendance.entity.Student;
import com.smartattendance.enums.EnrollmentStatus;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.EnrollmentRepository;
import com.smartattendance.repository.StudentRepository;
import com.smartattendance.service.EnrollmentService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Override
    public EnrollmentResponse createEnrollment(
            EnrollmentRequest request) {

        // Check if student exists
        Student student = studentRepository.findById(
                request.getStudentId()
        ).orElseThrow(() ->
                new RuntimeException("Student not found.")
        );

        // Check if course exists
        Course course = courseRepository.findById(
                request.getCourseId()
        ).orElseThrow(() ->
                new RuntimeException("Course not found.")
        );

        // Prevent duplicate enrollment
        if (enrollmentRepository
                .existsByStudentIdAndCourseId(
                        request.getStudentId(),
                        request.getCourseId()
                )) {

            throw new RuntimeException(
                    "Student is already enrolled in this course."
            );
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .enrollmentDate(
                        java.time.LocalDate.now()
                )
                .status(EnrollmentStatus.ACTIVE)
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        return mapToResponse(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getAllEnrollments() {

        return enrollmentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(
            Long id) {

        Enrollment enrollment =
                enrollmentRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Enrollment not found."
                                )
                        );

        return mapToResponse(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getEnrollmentsByStudent(
            Long studentId) {

        return enrollmentRepository
                .findByStudentId(studentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getEnrollmentsByCourse(
            Long courseId) {

        return enrollmentRepository
                .findByCourseId(courseId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public EnrollmentResponse updateEnrollmentStatus(
            Long id,
            String status) {

        Enrollment enrollment =
                enrollmentRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Enrollment not found."
                                )
                        );

        EnrollmentStatus enrollmentStatus;

        try {
            enrollmentStatus =
                    EnrollmentStatus.valueOf(
                            status.toUpperCase()
                    );
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Invalid enrollment status. " +
                    "Use ACTIVE, COMPLETED or DROPPED."
            );
        }

        enrollment.setStatus(enrollmentStatus);

        enrollment =
                enrollmentRepository.save(enrollment);

        return mapToResponse(enrollment);
    }

    @Override
    public void deleteEnrollment(Long id) {

        if (!enrollmentRepository.existsById(id)) {

            throw new RuntimeException(
                    "Enrollment not found."
            );
        }

        enrollmentRepository.deleteById(id);
    }

    private EnrollmentResponse mapToResponse(
            Enrollment enrollment) {

        return EnrollmentResponse.builder()
                .id(enrollment.getId())

                .studentId(
                        enrollment.getStudent().getId()
                )

                .studentName(
                        enrollment.getStudent()
                                .getUser()
                                .getFullName()
                )

                .registrationNumber(
                        enrollment.getStudent()
                                .getRegistrationNumber()
                )

                .courseId(
                        enrollment.getCourse().getId()
                )

                .courseCode(
                        enrollment.getCourse().getCourseCode()
                )

                .courseName(
                        enrollment.getCourse().getCourseName()
                )

                .enrollmentDate(
                        enrollment.getEnrollmentDate()
                )

                .status(
                        enrollment.getStatus()
                )

                .build();
    }
}