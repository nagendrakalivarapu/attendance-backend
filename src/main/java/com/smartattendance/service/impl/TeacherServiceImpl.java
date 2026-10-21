package com.smartattendance.service.impl;

import com.smartattendance.dto.request.TeacherRequest;
import com.smartattendance.dto.response.TeacherResponse;
import com.smartattendance.entity.Teacher;
import com.smartattendance.entity.User;
import com.smartattendance.enums.UserRole;
import com.smartattendance.repository.TeacherRepository;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public TeacherResponse createTeacher(TeacherRequest request) {

        // Check Employee ID
        if (teacherRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new RuntimeException("Employee ID already exists.");
        }

        // Check Email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists.");
        }

        // Create User account
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.TEACHER)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        // Create Teacher profile
        Teacher teacher = Teacher.builder()
                .employeeId(request.getEmployeeId())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .phone(request.getPhone())
                .qualification(request.getQualification())
                .user(user)
                .build();

        teacher = teacherRepository.save(teacher);

        return mapToResponse(teacher);
    }

    @Override
    public List<TeacherResponse> getAllTeachers() {
        return teacherRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public TeacherResponse getTeacherById(Long id) {

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found."));

        return mapToResponse(teacher);
    }

    @Override
    public TeacherResponse updateTeacher(Long id, TeacherRequest request) {

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found."));

        User user = teacher.getUser();

        // Update User information
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        // Update password only if provided
        if (request.getPassword() != null &&
                !request.getPassword().isBlank()) {

            user.setPassword(
                    passwordEncoder.encode(request.getPassword())
            );
        }

        userRepository.save(user);

        // Update Teacher information
        teacher.setEmployeeId(request.getEmployeeId());
        teacher.setDepartment(request.getDepartment());
        teacher.setDesignation(request.getDesignation());
        teacher.setPhone(request.getPhone());
        teacher.setQualification(request.getQualification());

        teacher = teacherRepository.save(teacher);

        return mapToResponse(teacher);
    }

    @Override
    public void deleteTeacher(Long id) {

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found."));

        teacherRepository.delete(teacher);
    }

    private TeacherResponse mapToResponse(Teacher teacher) {

        return TeacherResponse.builder()
                .id(teacher.getId())
                .employeeId(teacher.getEmployeeId())
                .department(teacher.getDepartment())
                .designation(teacher.getDesignation())
                .phone(teacher.getPhone())
                .qualification(teacher.getQualification())
                .fullName(teacher.getUser().getFullName())
                .email(teacher.getUser().getEmail())
                .build();
    }
}