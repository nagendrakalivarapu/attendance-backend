package com.smartattendance.service;

import com.smartattendance.dto.request.CourseRequest;
import com.smartattendance.dto.response.CourseResponse;

import java.util.List;

public interface CourseService {

    CourseResponse createCourse(CourseRequest request);

    List<CourseResponse> getAllCourses();

    CourseResponse getCourseById(Long id);

    CourseResponse updateCourse(Long id, CourseRequest request);

    void deleteCourse(Long id);

    List<CourseResponse> getCoursesByDepartment(String department);

    List<CourseResponse> getCoursesBySemester(Integer semester);

    List<CourseResponse> getCoursesByTeacher(Long teacherId);
}