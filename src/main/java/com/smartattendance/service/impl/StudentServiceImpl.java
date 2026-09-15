package com.smartattendance.service.impl;

import com.smartattendance.dto.request.StudentRequest;
import com.smartattendance.dto.response.StudentResponse;
import com.smartattendance.entity.Student;
import com.smartattendance.entity.User;
import com.smartattendance.repository.StudentRepository;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.service.StudentService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    @Override
    public StudentResponse createStudent(StudentRequest request) {

        // Check duplicate registration number
        if (studentRepository.existsByRegistrationNumber(
                request.getRegistrationNumber())) {

            throw new RuntimeException(
                    "Registration number already exists."
            );
        }

        // Find user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new RuntimeException("User not found.")
                );

        // Create student
        Student student = Student.builder()
                .registrationNumber(request.getRegistrationNumber())
                .department(request.getDepartment())
                .semester(request.getSemester())
                .section(request.getSection())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .user(user)
                .build();

        student = studentRepository.save(student);

        return mapToResponse(student);
    }

    @Override
    public List<StudentResponse> getAllStudents() {

        return studentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public StudentResponse getStudentById(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Student not found.")
                );

        return mapToResponse(student);
    }

    @Override
    public StudentResponse updateStudent(
            Long id,
            StudentRequest request) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Student not found.")
                );

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new RuntimeException("User not found.")
                );

        student.setRegistrationNumber(
                request.getRegistrationNumber()
        );

        student.setDepartment(
                request.getDepartment()
        );

        student.setSemester(
                request.getSemester()
        );

        student.setSection(
                request.getSection()
        );

        student.setPhone(
                request.getPhone()
        );

        student.setDateOfBirth(
                request.getDateOfBirth()
        );

        student.setGender(
                request.getGender()
        );

        student.setAddress(
                request.getAddress()
        );

        student.setUser(user);

        student = studentRepository.save(student);

        return mapToResponse(student);
    }

    @Override
    public void deleteStudent(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Student not found.")
                );

        studentRepository.delete(student);
    }

    private StudentResponse mapToResponse(Student student) {

        return StudentResponse.builder()
                .id(student.getId())
                .registrationNumber(
                        student.getRegistrationNumber()
                )
                .department(
                        student.getDepartment()
                )
                .semester(
                        student.getSemester()
                )
                .section(
                        student.getSection()
                )
                .phone(
                        student.getPhone()
                )
                .dateOfBirth(
                        student.getDateOfBirth()
                )
                .gender(
                        student.getGender()
                )
                .address(
                        student.getAddress()
                )
                .studentName(
                        student.getUser().getFullName()
                )
                .email(
                        student.getUser().getEmail()
                )
                .faceRegistered(
                        student.getFaceRegistered()
                )
                .build();
    }
}