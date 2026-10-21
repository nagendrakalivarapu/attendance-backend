package com.smartattendance.service.impl;

import com.smartattendance.dto.request.StudentRequest;
import com.smartattendance.dto.response.StudentResponse;
import com.smartattendance.entity.Student;
import com.smartattendance.entity.User;
import com.smartattendance.repository.StudentRepository;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.service.FaceRecognitionService;
import com.smartattendance.service.StudentService;
import com.smartattendance.enums.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    private final UserRepository userRepository;

    private final FaceRecognitionService faceRecognitionService;
    private final PasswordEncoder passwordEncoder;

    // ============================================================
    // CREATE STUDENT
    // ============================================================

    @Override
    public StudentResponse createStudent(StudentRequest request) {

        // Check duplicate registration number
        if (studentRepository.existsByRegistrationNumber(
                request.getRegistrationNumber())) {

            throw new RuntimeException(
                    "Registration number already exists."
            );
        }

        // Check duplicate email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {

            throw new RuntimeException(
                    "Email already exists."
            );
        }

        // ========================================================
        // CREATE USER ACCOUNT
        // ========================================================

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.STUDENT)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        // ========================================================
        // CREATE STUDENT
        // ========================================================

        Student student = Student.builder()
                .registrationNumber(
                        request.getRegistrationNumber()
                )
                .department(
                        request.getDepartment()
                )
                .semester(
                        request.getSemester()
                )
                .section(
                        request.getSection()
                )
                .phone(
                        request.getPhone()
                )
                .dateOfBirth(
                        request.getDateOfBirth()
                )
                .gender(
                        request.getGender()
                )
                .address(
                        request.getAddress()
                )
                .user(user)
                .build();

        student = studentRepository.save(student);

        return mapToResponse(student);
    }


    // ============================================================
    // GET ALL STUDENTS
    // ============================================================

    @Override
    public List<StudentResponse> getAllStudents() {

        return studentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // ============================================================
    // GET STUDENT BY ID
    // ============================================================

    @Override
    public StudentResponse getStudentById(
            Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found."
                        )
                );

        return mapToResponse(student);
    }


    // ============================================================
    // UPDATE STUDENT
    // ============================================================

    @Override
    public StudentResponse updateStudent(
            Long id,
            StudentRequest request) {

        // Find existing student
        Student student =
                studentRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found."
                                )
                        );

        // Get the User already linked to this student
        User user = student.getUser();

        if (user == null) {
            throw new RuntimeException(
                    "Student does not have a linked user account."
            );
        }

        // ============================================================
        // UPDATE STUDENT INFORMATION
        // ============================================================

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

        // ============================================================
        // UPDATE USER INFORMATION
        // ============================================================

        // Update name
        if (request.getFullName() != null &&
                !request.getFullName().isBlank()) {

            user.setFullName(
                    request.getFullName()
            );
        }

        // Update email
        if (request.getEmail() != null &&
                !request.getEmail().isBlank() &&
                !request.getEmail().equals(user.getEmail())) {

            if (userRepository.existsByEmail(
                    request.getEmail())) {

                throw new RuntimeException(
                        "Email already exists."
                );
            }

            user.setEmail(
                    request.getEmail()
            );
        }

        // Update password only if a new password was entered
        if (request.getPassword() != null &&
                !request.getPassword().isBlank()) {

            user.setPassword(
                    passwordEncoder.encode(
                            request.getPassword()
                    )
            );
        }

        // Save User
        userRepository.save(user);

        // Save Student
        student = studentRepository.save(student);

        return mapToResponse(student);
    }


    // ============================================================
    // DELETE STUDENT
    // ============================================================

    @Override
    public void deleteStudent(
            Long id) {

        Student student =
                studentRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found."
                                )
                        );

        studentRepository.delete(student);
    }


    // ============================================================
    // REGISTER FACE
    // ============================================================

    @Override
    public boolean registerFace(
            Long studentId,
            MultipartFile image) {

        // Check image
        if (image == null || image.isEmpty()) {

            throw new RuntimeException(
                    "Face image is required."
            );
        }

        // Find student
        Student student =
                studentRepository.findById(studentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found."
                                )
                        );

        // Get registration number
        String registrationNumber =
                student.getRegistrationNumber();

        // Send image to FastAPI
        boolean registered =
                faceRecognitionService.registerFace(
                        registrationNumber,
                        image
                );

        // FastAPI registration failed
        if (!registered) {

            throw new RuntimeException(
                    "Face registration failed. "
                    + "Please make sure the image contains "
                    + "exactly one face."
            );
        }

        // Mark face as registered
        student.setFaceRegistered(true);

        // Save updated student
        studentRepository.save(student);

        return true;
    }


    // ============================================================
    // MAP ENTITY TO RESPONSE
    // ============================================================

    private StudentResponse mapToResponse(
            Student student) {

        return StudentResponse.builder()

                .id(
                        student.getId()
                )

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