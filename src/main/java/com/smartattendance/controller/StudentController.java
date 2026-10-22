package com.smartattendance.controller;

import com.smartattendance.dto.request.StudentRequest;
import com.smartattendance.dto.response.StudentResponse;
import com.smartattendance.service.StudentService;
import org.springframework.security.core.Authentication;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;


    // ============================================================
    // CREATE STUDENT
    // ============================================================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse createStudent(
            @Valid @RequestBody StudentRequest request) {

        return studentService.createStudent(request);
    }


    // ============================================================
    // GET ALL STUDENTS
    // ============================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<StudentResponse> getAllStudents() {

        return studentService.getAllStudents();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentResponse getMyProfile(Authentication authentication) {
        return studentService.getMyProfile(authentication.getName());
    }

    // ============================================================
    // GET STUDENT BY ID
    // ============================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public StudentResponse getStudentById(
            @PathVariable Long id) {

        return studentService.getStudentById(id);
    }



    // ============================================================
    // UPDATE STUDENT
    // ============================================================

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request) {

        return studentService.updateStudent(
                id,
                request
        );
    }


    // ============================================================
    // DELETE STUDENT
    // ============================================================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteStudent(
            @PathVariable Long id) {

        studentService.deleteStudent(id);
    }


    // ============================================================
    // REGISTER / UPDATE FACE
    // ============================================================

    @PostMapping(
            value = "/{id}/face",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> registerFace(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {

        // Validate image
        if (image == null || image.isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body("Face image is required.");
        }


        // Register face
        boolean registered =
                studentService.registerFace(
                        id,
                        image
                );


        // Face registration failed
        if (!registered) {

            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("Face registration failed.");
        }


        // Face registration successful
        return ResponseEntity
                .ok("Face registered successfully.");
    }
}