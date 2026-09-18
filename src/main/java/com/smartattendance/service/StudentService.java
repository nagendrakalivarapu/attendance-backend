package com.smartattendance.service;

import com.smartattendance.dto.request.StudentRequest;
import com.smartattendance.dto.response.StudentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StudentService {

    StudentResponse createStudent(StudentRequest request);

    List<StudentResponse> getAllStudents();

    StudentResponse getStudentById(Long id);

    StudentResponse updateStudent(Long id, StudentRequest request);

    void deleteStudent(Long id);

    boolean registerFace(Long studentId, MultipartFile image);
}