package com.smartattendance.service.impl;

import com.smartattendance.dto.request.EnrollmentRequest;
import com.smartattendance.dto.response.EnrollmentResponse;
import com.smartattendance.entity.Course;
import com.smartattendance.entity.Enrollment;
import com.smartattendance.entity.Student;
import com.smartattendance.entity.Teacher;
import com.smartattendance.enums.EnrollmentStatus;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.EnrollmentRepository;
import com.smartattendance.repository.StudentRepository;
import com.smartattendance.repository.TeacherRepository;
import com.smartattendance.service.EnrollmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;

    @Override
    public EnrollmentResponse createEnrollment(EnrollmentRequest request) {

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found."));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found."));

        if (enrollmentRepository.existsByStudentIdAndCourseId(
                request.getStudentId(), request.getCourseId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student is already enrolled in this course.");
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .enrollmentDate(java.time.LocalDate.now())
                .status(EnrollmentStatus.ACTIVE)
                .build();

        return mapToResponse(enrollmentRepository.save(enrollment));
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
            Long id, String email, boolean isAdmin) {

        Enrollment enrollment = findEnrollment(id);

        if (!isAdmin) {
            verifyTeacherOwnsCourse(enrollment.getCourse(), email);
        }

        return mapToResponse(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getEnrollmentsByStudent(
            Long studentId, String email, boolean isAdmin) {

        if (!studentRepository.existsById(studentId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Student not found.");
        }

        List<Enrollment> enrollments;

        if (isAdmin) {
            enrollments = enrollmentRepository.findByStudentId(studentId);
        } else {
            Teacher teacher = findTeacherByEmail(email);
            enrollments = enrollmentRepository
                    .findByStudentIdAndCourse_Teacher_Id(
                            studentId, teacher.getId());
        }

        return enrollments.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyEnrollments(String email) {

        Student student = studentRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found for logged-in user."));

        return enrollmentRepository.findByStudentId(student.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getTeacherEnrollments(String email) {

        Teacher teacher = findTeacherByEmail(email);

        return enrollmentRepository
                .findByCourse_Teacher_Id(teacher.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getEnrollmentsByCourse(
            Long courseId, String email, boolean isAdmin) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found."));

        if (!isAdmin) {
            verifyTeacherOwnsCourse(course, email);
        }

        return enrollmentRepository.findByCourseId(courseId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public EnrollmentResponse updateEnrollmentStatus(
            Long id, String status) {

        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Enrollment status is required.");
        }

        Enrollment enrollment = findEnrollment(id);

        EnrollmentStatus enrollmentStatus;

        try {
            enrollmentStatus = EnrollmentStatus.valueOf(
                    status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status. Use ACTIVE, COMPLETED or DROPPED.");
        }

        enrollment.setStatus(enrollmentStatus);

        return mapToResponse(enrollmentRepository.save(enrollment));
    }

    @Override
    public void deleteEnrollment(Long id) {
        Enrollment enrollment = findEnrollment(id);
        enrollmentRepository.delete(enrollment);
    }

    private Enrollment findEnrollment(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Enrollment not found."));
    }

    private Teacher findTeacherByEmail(String email) {
        return teacherRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Teacher not found for logged-in user."));
    }

    private void verifyTeacherOwnsCourse(Course course, String email) {

        if (course.getTeacher() == null ||
                !course.getTeacher().getUser().getEmail()
                        .equalsIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not assigned to this course.");
        }
    }

    private EnrollmentResponse mapToResponse(Enrollment enrollment) {

        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent()
                        .getUser().getFullName())
                .registrationNumber(enrollment.getStudent()
                        .getRegistrationNumber())
                .courseId(enrollment.getCourse().getId())
                .courseCode(enrollment.getCourse().getCourseCode())
                .courseName(enrollment.getCourse().getCourseName())
                .enrollmentDate(enrollment.getEnrollmentDate())
                .status(enrollment.getStatus())
                .build();
    }
}