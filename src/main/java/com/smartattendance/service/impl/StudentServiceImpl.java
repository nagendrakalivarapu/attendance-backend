package com.smartattendance.service.impl;

import com.smartattendance.dto.request.StudentRequest;
import com.smartattendance.dto.response.StudentResponse;
import com.smartattendance.entity.Student;
import com.smartattendance.entity.User;
import com.smartattendance.enums.UserRole;
import com.smartattendance.repository.AttendanceRepository;
import com.smartattendance.repository.EnrollmentRepository;
import com.smartattendance.repository.StudentRepository;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.service.FaceRecognitionService;
import com.smartattendance.service.StudentService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;

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
    // GET MY PROFILE
    // ============================================================

    @Override
    public StudentResponse getMyProfile(String email) {

        Student student = studentRepository
                .findByUser_Email(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found for logged-in user."
                        )
                );

        return mapToResponse(student);
    }


    // ============================================================
    // GET STUDENT BY ID
    // ============================================================

    @Override
    public StudentResponse getStudentById(Long id) {

        Student student = studentRepository
                .findById(id)
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
                studentRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found."
                                )
                        );

        // Get linked User
        User user = student.getUser();

        if (user == null) {
            throw new RuntimeException(
                    "Student does not have a linked user account."
            );
        }


        // ========================================================
        // UPDATE STUDENT INFORMATION
        // ========================================================

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


        // ========================================================
        // UPDATE USER INFORMATION
        // ========================================================

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


        // Update password only if provided
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
    @Transactional
    public void deleteStudent(Long id) {

        // --------------------------------------------------------
        // 1. FIND STUDENT
        // --------------------------------------------------------

        Student student = studentRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found."
                        )
                );


        // --------------------------------------------------------
        // 2. SAVE REQUIRED INFORMATION BEFORE DELETION
        // --------------------------------------------------------

        String registrationNumber =
                student.getRegistrationNumber();

        String profilePicture =
                student.getProfilePicture();

        User user =
                student.getUser();


        // --------------------------------------------------------
        // 3. DELETE ATTENDANCE RECORDS
        // --------------------------------------------------------

        attendanceRepository.deleteByStudentId(id);


        // --------------------------------------------------------
        // 4. DELETE ENROLLMENTS
        // --------------------------------------------------------

        enrollmentRepository.deleteByStudentId(id);


        // --------------------------------------------------------
        // 5. DELETE STUDENT RECORD
        // --------------------------------------------------------

        studentRepository.delete(student);


        // --------------------------------------------------------
        // 6. DELETE LINKED USER ACCOUNT
        // --------------------------------------------------------

        if (user != null) {

            userRepository.delete(user);
        }


        // --------------------------------------------------------
        // 7. DELETE FACE RECOGNITION FILES
        // --------------------------------------------------------

        deleteFaceFiles(registrationNumber);


        // --------------------------------------------------------
        // 8. DELETE PROFILE PICTURE
        // --------------------------------------------------------

        deleteProfilePicture(profilePicture);
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
                studentRepository
                        .findById(studentId)
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
    // UPLOAD PROFILE PICTURE
    // ============================================================

    @Override
    public boolean uploadProfilePicture(
            Long studentId,
            MultipartFile image) {

        Student student =
                studentRepository
                        .findById(studentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found."
                                )
                        );


        // Validate image
        if (image == null || image.isEmpty()) {

            throw new RuntimeException(
                    "Profile picture is required."
            );
        }


        // Validate content type
        String contentType =
                image.getContentType();

        if (contentType == null ||
                !(contentType.equals("image/jpeg")
                        || contentType.equals("image/png")
                        || contentType.equals("image/webp"))) {

            throw new RuntimeException(
                    "Only JPG, PNG, and WEBP images are allowed."
            );
        }


        // Validate file size
        if (image.getSize() > 5 * 1024 * 1024) {

            throw new RuntimeException(
                    "Profile picture must be less than 5 MB."
            );
        }


        try {

            // ----------------------------------------------------
            // CREATE UPLOAD DIRECTORY
            // ----------------------------------------------------

            Path uploadDirectory =
                    Paths.get(
                            "uploads/profile-pictures"
                    );

            Files.createDirectories(
                    uploadDirectory
            );


            // ----------------------------------------------------
            // DELETE PREVIOUS PROFILE PICTURE
            // ----------------------------------------------------

            if (student.getProfilePicture() != null) {

                Path oldFile =
                        Paths.get(
                                student.getProfilePicture()
                        );

                if (Files.exists(oldFile)) {

                    Files.delete(oldFile);
                }
            }


            // ----------------------------------------------------
            // GET FILE EXTENSION
            // ----------------------------------------------------

            String originalFilename =
                    image.getOriginalFilename();

            String extension = ".jpg";

            if (originalFilename != null &&
                    originalFilename.contains(".")) {

                extension =
                        originalFilename.substring(
                                originalFilename.lastIndexOf(".")
                        ).toLowerCase();
            }


            // ----------------------------------------------------
            // CREATE UNIQUE FILE NAME
            // ----------------------------------------------------

            String filename =
                    UUID.randomUUID()
                            + extension;


            Path targetPath =
                    uploadDirectory.resolve(
                            filename
                    );


            // ----------------------------------------------------
            // SAVE FILE
            // ----------------------------------------------------

            Files.copy(
                    image.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );


            // ----------------------------------------------------
            // SAVE PATH IN DATABASE
            // ----------------------------------------------------

            student.setProfilePicture(
                    targetPath.toString()
            );

            studentRepository.save(student);

            return true;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to save profile picture.",
                    e
            );
        }
    }


    // ============================================================
    // DELETE FACE RECOGNITION FILES
    // ============================================================

    private void deleteFaceFiles(
            String registrationNumber) {

        if (registrationNumber == null ||
                registrationNumber.isBlank()) {

            return;
        }


        String[] files = {

                "face-recognition-api/embeddings/"
                        + registrationNumber
                        + ".npy",

                "face-recognition-api/uploads/"
                        + registrationNumber
                        + ".jpg"
        };


        for (String filePath : files) {

            try {

                Path path =
                        Paths.get(filePath);

                Files.deleteIfExists(path);

            } catch (Exception e) {

                // Do not fail database deletion
                // because of face-file cleanup problem.

                System.err.println(
                        "Unable to delete face file: "
                                + filePath
                );
            }
        }
    }


    // ============================================================
    // DELETE PROFILE PICTURE
    // ============================================================

    private void deleteProfilePicture(
            String profilePicture) {

        if (profilePicture == null ||
                profilePicture.isBlank()) {

            return;
        }


        try {

            Path path =
                    Paths.get(profilePicture);

            Files.deleteIfExists(path);

        } catch (Exception e) {

            // Do not fail database deletion
            // because of profile picture cleanup problem.

            System.err.println(
                    "Unable to delete profile picture: "
                            + profilePicture
            );
        }
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

                .profilePicture(
                        student.getProfilePicture()
                )

                .build();
    }
}