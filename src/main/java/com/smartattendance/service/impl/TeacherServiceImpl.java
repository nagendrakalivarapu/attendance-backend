
package com.smartattendance.service.impl;

import com.smartattendance.dto.request.TeacherRequest;
import com.smartattendance.dto.response.TeacherResponse;
import com.smartattendance.entity.Teacher;
import com.smartattendance.entity.User;
import com.smartattendance.enums.UserRole;
import com.smartattendance.repository.TeacherRepository;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.service.TeacherService;

import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------
    // CREATE TEACHER
    // ------------------------------------------------------------

    @Override
    @Transactional
    public TeacherResponse createTeacher(TeacherRequest request) {

        if (request == null) {
            throw badRequest("Teacher details are required.");
        }

        String employeeId = normalizeRequired(
                request.getEmployeeId(),
                "Employee ID is required.");

        String email = normalizeEmail(request.getEmail());

        String fullName = normalizeRequired(
                request.getFullName(),
                "Teacher name is required.");

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {
            throw badRequest("Teacher password is required.");
        }

        if (teacherRepository.existsByEmployeeId(employeeId)) {
            throw conflict("Employee ID already exists.");
        }

        if (userRepository.existsByEmail(email)) {
            throw conflict("Email already exists.");
        }

        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.TEACHER)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        Teacher teacher = Teacher.builder()
                .employeeId(employeeId)
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .phone(request.getPhone())
                .qualification(request.getQualification())
                .user(user)
                .build();

        teacher = teacherRepository.save(teacher);

        return mapToResponse(teacher);
    }

    // ------------------------------------------------------------
    // GET ALL TEACHERS
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<TeacherResponse> getAllTeachers() {

        return teacherRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ------------------------------------------------------------
    // GET TEACHER BY ID
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getTeacherById(Long id) {

        Teacher teacher = findTeacher(id);

        return mapToResponse(teacher);
    }

    // ------------------------------------------------------------
    // UPDATE TEACHER
    // ------------------------------------------------------------

    @Override
    @Transactional
    public TeacherResponse updateTeacher(
            Long id,
            TeacherRequest request) {

        if (request == null) {
            throw badRequest("Teacher details are required.");
        }

        Teacher teacher = findTeacher(id);

        User user = teacher.getUser();

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Teacher does not have a linked user account.");
        }

        String employeeId = normalizeRequired(
                request.getEmployeeId(),
                "Employee ID is required.");

        String email = normalizeEmail(request.getEmail());

        String fullName = normalizeRequired(
                request.getFullName(),
                "Teacher name is required.");

        // Check employee ID only if it has changed.
        if (!employeeId.equals(teacher.getEmployeeId())
                && teacherRepository
                .existsByEmployeeIdAndIdNot(employeeId, id)) {

            throw conflict("Employee ID already exists.");
        }

        // Check email only if it has changed.
        if (!email.equalsIgnoreCase(user.getEmail())) {

            if (userRepository.existsByEmail(email)) {
                throw conflict("Email already exists.");
            }

            user.setEmail(email);
        }

        // Update user details.
        user.setFullName(fullName);

        // Update password only when a new password is provided.
        if (request.getPassword() != null
                && !request.getPassword().isBlank()) {

            user.setPassword(
                    passwordEncoder.encode(request.getPassword()));
        }

        // Update teacher details.
        teacher.setEmployeeId(employeeId);
        teacher.setDepartment(request.getDepartment());
        teacher.setDesignation(request.getDesignation());
        teacher.setPhone(request.getPhone());
        teacher.setQualification(request.getQualification());

        userRepository.save(user);

        teacher = teacherRepository.save(teacher);

        return mapToResponse(teacher);
    }

    // ------------------------------------------------------------
    // DELETE TEACHER
    // ------------------------------------------------------------

    @Override
    @Transactional
    public void deleteTeacher(Long id) {

        Teacher teacher = findTeacher(id);

        User user = teacher.getUser();

        // Delete the teacher profile first.
        teacherRepository.delete(teacher);

        // Delete the linked login account as well.
        if (user != null) {
            userRepository.delete(user);
        }
    }

    // ------------------------------------------------------------
    // FIND TEACHER
    // ------------------------------------------------------------

    private Teacher findTeacher(Long id) {

        if (id == null) {
            throw badRequest("Teacher ID is required.");
        }

        return teacherRepository.findById(id)
                .orElseThrow(() -> notFound("Teacher not found."));
    }

    // ------------------------------------------------------------
    // VALIDATION HELPERS
    // ------------------------------------------------------------

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw badRequest("Email is required.");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(
            String value,
            String message) {

        if (value == null || value.isBlank()) {
            throw badRequest(message);
        }

        return value.trim();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, message);
    }

    // ------------------------------------------------------------
    // ENTITY TO RESPONSE
    // ------------------------------------------------------------

    private TeacherResponse mapToResponse(Teacher teacher) {

        User user = teacher.getUser();

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Teacher has no linked user account.");
        }

        return TeacherResponse.builder()
                .id(teacher.getId())
                .employeeId(teacher.getEmployeeId())
                .department(teacher.getDepartment())
                .designation(teacher.getDesignation())
                .phone(teacher.getPhone())
                .qualification(teacher.getQualification())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }
}
