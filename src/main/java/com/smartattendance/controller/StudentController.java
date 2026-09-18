package com.smartattendance.controller;

import com.smartattendance.dto.request.StudentRequest;
import com.smartattendance.dto.response.StudentResponse;
import com.smartattendance.service.StudentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse createStudent(
            @Valid @RequestBody StudentRequest request) {

        return studentService.createStudent(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<StudentResponse> getAllStudents() {

        return studentService.getAllStudents();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public StudentResponse getStudentById(
            @PathVariable Long id) {

        return studentService.getStudentById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request) {

        return studentService.updateStudent(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteStudent(
            @PathVariable Long id) {

        studentService.deleteStudent(id);
    }
    @PostMapping(
            value = "/{id}/face",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> registerFace(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {

        studentService.registerFace(id, image);

        return ResponseEntity.ok(
                "Face registered successfully."
        );
    }
}