
package com.smartattendance.controller;

import com.smartattendance.dto.request.TeacherRequest;
import com.smartattendance.dto.response.TeacherResponse;
import com.smartattendance.service.TeacherService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    // CREATE TEACHER - ADMIN ONLY
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeacherResponse> createTeacher(
            @Valid @RequestBody TeacherRequest request) {

        TeacherResponse response =
                teacherService.createTeacher(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET ALL TEACHERS
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<TeacherResponse> getAllTeachers() {

        return teacherService.getAllTeachers();
    }

    // GET TEACHER BY ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public TeacherResponse getTeacherById(
            @PathVariable Long id) {

        return teacherService.getTeacherById(id);
    }

    // UPDATE TEACHER - ADMIN ONLY
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherResponse updateTeacher(
            @PathVariable Long id,
            @Valid @RequestBody TeacherRequest request) {

        return teacherService.updateTeacher(id, request);
    }

    // DELETE TEACHER - ADMIN ONLY
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTeacher(
            @PathVariable Long id) {

        teacherService.deleteTeacher(id);

        return ResponseEntity.noContent().build();
    }
}
