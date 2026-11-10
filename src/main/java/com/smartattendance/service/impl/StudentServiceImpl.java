
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
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import java.util.Iterator;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final FaceRecognitionService faceRecognitionService;
    private final PasswordEncoder passwordEncoder;

    @Value("${student.profile-picture.directory:uploads/profile-pictures}")
    private String profilePictureDirectory;

    // ------------------------------------------------------------
    // CREATE STUDENT
    // ------------------------------------------------------------

    @Override
    @Transactional
    public StudentResponse createStudent(StudentRequest request) {

        if (request == null) {
            throw badRequest("Student details are required.");
        }

        String registrationNumber =
                normalizeRequired(request.getRegistrationNumber(),
                        "Registration number is required.");

        String email = normalizeEmail(request.getEmail());

        String password = request.getPassword();

        if (password == null || password.isBlank()) {
            throw badRequest("Student password is required.");
        }

        if (studentRepository.existsByRegistrationNumber(
                registrationNumber)) {
            throw conflict("Registration number already exists.");
        }

        if (userRepository.existsByEmail(email)) {
            throw conflict("Email already exists.");
        }

        User user = User.builder()
                .fullName(normalizeRequired(
                        request.getFullName(), "Student name is required."))
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(UserRole.STUDENT)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        Student student = Student.builder()
                .registrationNumber(registrationNumber)
                .department(request.getDepartment())
                .semester(request.getSemester())
                .section(request.getSection())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .user(user)
                .build();

        return mapToResponse(studentRepository.save(student));
    }

    // ------------------------------------------------------------
    // GET ALL STUDENTS
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public java.util.List<StudentResponse> getAllStudents() {

        return studentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ------------------------------------------------------------
    // GET MY PROFILE
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getMyProfile(String email) {

        String normalizedEmail = normalizeEmail(email);

        Student student = studentRepository
                .findByUser_Email(normalizedEmail)
                .orElseThrow(() -> notFound(
                        "Student profile not found for logged-in user."));

        return mapToResponse(student);
    }

    // ------------------------------------------------------------
    // GET STUDENT BY ID
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {

        Student student = findStudent(id);

        return mapToResponse(student);
    }

    // ------------------------------------------------------------
    // UPDATE STUDENT
    // ------------------------------------------------------------

    @Override
    @Transactional
    public StudentResponse updateStudent(
            Long id,
            StudentRequest request) {

        if (request == null) {
            throw badRequest("Student details are required.");
        }

        Student student = findStudent(id);

        User user = student.getUser();

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student does not have a linked user account.");
        }

        String registrationNumber = normalizeRequired(
                request.getRegistrationNumber(),
                "Registration number is required.");

        if (!registrationNumber.equals(
                student.getRegistrationNumber())
                && studentRepository.existsByRegistrationNumberAndIdNot(
                registrationNumber, id)) {

            throw conflict("Registration number already exists.");
        }

        String email = normalizeEmail(request.getEmail());

        if (!email.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw conflict("Email already exists.");
            }
            user.setEmail(email);
        }

        student.setRegistrationNumber(registrationNumber);
        student.setDepartment(request.getDepartment());
        student.setSemester(request.getSemester());
        student.setSection(request.getSection());
        student.setPhone(request.getPhone());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setGender(request.getGender());
        student.setAddress(request.getAddress());

        if (request.getFullName() != null
                && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getPassword() != null
                && !request.getPassword().isBlank()) {
            user.setPassword(
                    passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);

        return mapToResponse(studentRepository.save(student));
    }

    // ------------------------------------------------------------
    // DELETE STUDENT
    // ------------------------------------------------------------

    @Override
    @Transactional
    public void deleteStudent(Long id) {

        Student student = findStudent(id);

        String registrationNumber = student.getRegistrationNumber();
        String profilePicture = student.getProfilePicture();
        User user = student.getUser();

        attendanceRepository.deleteByStudentId(id);
        enrollmentRepository.deleteByStudentId(id);

        studentRepository.delete(student);

        if (user != null) {
            userRepository.delete(user);
        }

        // File cleanup is best-effort. It must never delete arbitrary
        // filesystem paths obtained from database values.
        deleteFaceFiles(registrationNumber);
        deleteProfilePicture(profilePicture);
    }

    // ------------------------------------------------------------
    // REGISTER FACE
    // ------------------------------------------------------------

    @Override
    @Transactional
    public boolean registerFace(Long studentId, MultipartFile image) {

        validateImage(image);

        Student student = findStudent(studentId);

        String registrationNumber = student.getRegistrationNumber();

        if (registrationNumber == null
                || !registrationNumber.matches("[A-Za-z0-9_-]{1,64}")) {
            throw badRequest("Invalid student registration number.");
        }

        boolean registered = faceRecognitionService.registerFace(
                registrationNumber, image);

        if (!registered) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Face registration failed. Ensure the image contains "
                            + "exactly one face.");
        }

        student.setFaceRegistered(true);
        studentRepository.save(student);

        return true;
    }

    // ------------------------------------------------------------
    // UPLOAD PROFILE PICTURE
    // ------------------------------------------------------------

    @Override
    @Transactional
    public boolean uploadProfilePicture(
            Long studentId,
            MultipartFile image) {

        validateImage(image);

        Student student = findStudent(studentId);

        Path uploadDirectory = Paths.get(profilePictureDirectory)
                .toAbsolutePath()
                .normalize();

        Path newFile = null;

        try {
            Files.createDirectories(uploadDirectory);

            String extension = getValidatedExtension(image);

            String filename = UUID.randomUUID() + extension;

            newFile = uploadDirectory.resolve(filename).normalize();

            if (!newFile.startsWith(uploadDirectory)) {
                throw badRequest("Invalid image storage path.");
            }

            Files.copy(image.getInputStream(), newFile);

            String oldPicture = student.getProfilePicture();

            student.setProfilePicture(newFile.toString());

            studentRepository.saveAndFlush(student);

            // Only delete the previous file after the new one is saved.
            deleteProfilePicture(oldPicture);

            return true;

        } catch (IOException ex) {

            deleteFileQuietly(newFile);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to save profile picture.");
        } catch (RuntimeException ex) {

            deleteFileQuietly(newFile);

            throw ex;
        }
    }

    // ------------------------------------------------------------
    // IMAGE VALIDATION
    // ------------------------------------------------------------


    private void validateImage(MultipartFile image) {

        if (image == null || image.isEmpty()) {
            throw badRequest("Image file is required.");
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image must not exceed 5 MB."
            );
        }

        String declaredType = image.getContentType();

        if (declaredType == null
                || !(declaredType.equalsIgnoreCase("image/jpeg")
                || declaredType.equalsIgnoreCase("image/png"))) {

            throw badRequest(
                    "Only JPEG and PNG images are allowed."
            );
        }

        try (ImageInputStream input =
                     ImageIO.createImageInputStream(image.getInputStream())) {

            if (input == null) {
                throw badRequest("Invalid image file.");
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw badRequest("Unsupported or invalid image file.");
            }

            ImageReader reader = readers.next();

            try {

                reader.setInput(input, true, true);

                String detectedFormat =
                        reader.getFormatName().toLowerCase(Locale.ROOT);

                boolean validFormat =
                        ("jpeg".equals(detectedFormat)
                                && declaredType.equalsIgnoreCase("image/jpeg"))
                                || ("png".equals(detectedFormat)
                                && declaredType.equalsIgnoreCase("image/png"));

                if (!validFormat) {
                    throw badRequest(
                            "Image content does not match its declared format."
                    );
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0 || height <= 0
                        || width > 4096 || height > 4096
                        || (long) width * height > 16_000_000L) {

                    throw badRequest(
                            "Image dimensions exceed the allowed limits."
                    );
                }

                BufferedImage decoded = reader.read(0);

                if (decoded == null) {
                    throw badRequest("Unable to decode image.");
                }

            } finally {
                reader.dispose();
            }

        } catch (IOException ex) {

            throw badRequest("Invalid or corrupted image file.");
        }
    }

    private String getValidatedExtension(MultipartFile image) {

        String contentType = image.getContentType();

        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            return ".jpg";
        }

        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }

        throw badRequest("Unsupported image type.");
    }


    // ------------------------------------------------------------
    // FILE CLEANUP
    // ------------------------------------------------------------

    private void deleteFaceFiles(String registrationNumber) {

        if (registrationNumber == null
                || !registrationNumber.matches("[A-Za-z0-9_-]{1,64}")) {
            return;
        }

        deleteFileQuietly(Paths.get(
                "face-recognition-api", "embeddings",
                registrationNumber + ".npy"));

        deleteFileQuietly(Paths.get(
                "face-recognition-api", "uploads",
                registrationNumber + ".jpg"));
    }

    private void deleteProfilePicture(String storedPath) {

        if (storedPath == null || storedPath.isBlank()) {
            return;
        }

        Path uploadDirectory = Paths.get(profilePictureDirectory)
                .toAbsolutePath()
                .normalize();

        try {
            Path file = Paths.get(storedPath)
                    .toAbsolutePath()
                    .normalize();

            if (file.startsWith(uploadDirectory)
                    && !file.equals(uploadDirectory)) {
                Files.deleteIfExists(file);
            }
        } catch (Exception ex) {
            // Best-effort cleanup; do not expose filesystem paths.
        }
    }

    private void deleteFileQuietly(Path path) {

        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            // Log this through a configured logger in production.
        }
    }

    // ------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------

    private Student findStudent(Long id) {

        if (id == null) {
            throw badRequest("Student ID is required.");
        }

        return studentRepository.findById(id)
                .orElseThrow(() -> notFound("Student not found."));
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw badRequest("Email is required.");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String message) {

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

    private StudentResponse mapToResponse(Student student) {

        User user = student.getUser();

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student has no linked user account.");
        }

        return StudentResponse.builder()
                .id(student.getId())
                .registrationNumber(student.getRegistrationNumber())
                .department(student.getDepartment())
                .semester(student.getSemester())
                .section(student.getSection())
                .phone(student.getPhone())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .address(student.getAddress())
                .studentName(user.getFullName())
                .email(user.getEmail())
                .faceRegistered(student.getFaceRegistered())
                .profilePicture(student.getProfilePicture())
                .build();
    }
}
