
package com.smartattendance.controller;

import com.smartattendance.dto.request.StudentRequest;
import com.smartattendance.dto.response.StudentResponse;
import com.smartattendance.service.StudentService;

import jakarta.validation.Valid;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // ============================================================
    // CREATE STUDENT - ADMIN ONLY
    // ============================================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentResponse> createStudent(
            @Valid @RequestBody StudentRequest request) {

        StudentResponse response =
                studentService.createStudent(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ============================================================
    // GET ALL STUDENTS - ADMIN ONLY
    // ============================================================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StudentResponse>> getAllStudents() {

        return ResponseEntity.ok(
                studentService.getAllStudents()
        );
    }

    // ============================================================
    // GET MY PROFILE - STUDENT ONLY
    // ============================================================

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentResponse> getMyProfile(
            Authentication authentication) {

        return ResponseEntity.ok(
                studentService.getMyProfile(
                        authentication.getName()
                )
        );
    }

    // ============================================================
    // GET STUDENT BY ID - ADMIN ONLY
    // ============================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentResponse> getStudentById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                studentService.getStudentById(id)
        );
    }

    // ============================================================
    // UPDATE STUDENT - ADMIN ONLY
    // ============================================================

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request) {

        return ResponseEntity.ok(
                studentService.updateStudent(id, request)
        );
    }

    // ============================================================
    // DELETE STUDENT - ADMIN ONLY
    // ============================================================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStudent(
            @PathVariable Long id) {

        studentService.deleteStudent(id);

        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // REGISTER / UPDATE FACE - ADMIN ONLY
    // ============================================================

    @PostMapping(
            value = "/{id}/face",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> registerFace(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {

        validateImage(image);

        boolean registered =
                studentService.registerFace(id, image);

        if (!registered) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("Face registration failed.");
        }

        return ResponseEntity.ok(
                "Face registered successfully."
        );
    }

    // ============================================================
    // UPLOAD PROFILE PICTURE - ADMIN ONLY
    // ============================================================

    @PostMapping(
            value = "/{id}/profile-picture",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadProfilePicture(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {

        validateImage(image);

        boolean uploaded =
                studentService.uploadProfilePicture(id, image);

        if (!uploaded) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("Profile picture upload failed.");
        }

        return ResponseEntity.ok(
                "Profile picture uploaded successfully."
        );
    }

    // ============================================================
    // COMMON IMAGE VALIDATION
    // ============================================================

    private void validateImage(MultipartFile image) {

        if (image == null || image.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image file is required."
            );
        }

        // Preliminary size check.
        if (image.getSize() > 5 * 1024 * 1024) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image must not exceed 5 MB."
            );
        }

        // Preliminary content type check.
        String contentType = image.getContentType();

        if (contentType == null ||
                !(contentType.equalsIgnoreCase("image/jpeg")
                        || contentType.equalsIgnoreCase("image/png")
                        || contentType.equalsIgnoreCase("image/webp"))) {

            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only JPG, PNG and WEBP images are allowed."
            );
        }
    }
}
