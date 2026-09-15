package com.smartattendance.service.impl;

import com.smartattendance.dto.request.CourseRequest;
import com.smartattendance.dto.response.CourseResponse;
import com.smartattendance.entity.Course;
import com.smartattendance.entity.Teacher;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.TeacherRepository;
import com.smartattendance.service.CourseService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;

    @Override
    public CourseResponse createCourse(CourseRequest request) {

        if (courseRepository.existsByCourseCode(
                request.getCourseCode())) {

            throw new RuntimeException(
                    "Course code already exists."
            );
        }

        Teacher teacher = null;

        if (request.getTeacherId() != null) {

            teacher = teacherRepository.findById(
                    request.getTeacherId()
            ).orElseThrow(() ->
                    new RuntimeException("Teacher not found.")
            );
        }

        Course course = Course.builder()
                .courseCode(request.getCourseCode())
                .courseName(request.getCourseName())
                .department(request.getDepartment())
                .semester(request.getSemester())
                .credits(request.getCredits())
                .totalClasses(request.getTotalClasses())
                .teacher(teacher)
                .build();

        course = courseRepository.save(course);

        return mapToResponse(course);
    }

    @Override
    public List<CourseResponse> getAllCourses() {

        return courseRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public CourseResponse getCourseById(Long id) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Course not found.")
                );

        return mapToResponse(course);
    }

    @Override
    public CourseResponse updateCourse(
            Long id,
            CourseRequest request) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Course not found.")
                );

        Teacher teacher = null;

        if (request.getTeacherId() != null) {

            teacher = teacherRepository.findById(
                    request.getTeacherId()
            ).orElseThrow(() ->
                    new RuntimeException("Teacher not found.")
            );
        }

        course.setCourseCode(request.getCourseCode());
        course.setCourseName(request.getCourseName());
        course.setDepartment(request.getDepartment());
        course.setSemester(request.getSemester());
        course.setCredits(request.getCredits());
        course.setTotalClasses(request.getTotalClasses());
        course.setTeacher(teacher);

        course = courseRepository.save(course);

        return mapToResponse(course);
    }

    @Override
    public void deleteCourse(Long id) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Course not found.")
                );

        courseRepository.delete(course);
    }

    @Override
    public List<CourseResponse> getCoursesByDepartment(
            String department) {

        return courseRepository
                .findByDepartment(department)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<CourseResponse> getCoursesBySemester(
            Integer semester) {

        return courseRepository
                .findBySemester(semester)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<CourseResponse> getCoursesByTeacher(
            Long teacherId) {

        return courseRepository
                .findByTeacherId(teacherId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private CourseResponse mapToResponse(Course course) {

        return CourseResponse.builder()
                .id(course.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .department(course.getDepartment())
                .semester(course.getSemester())
                .credits(course.getCredits())
                .totalClasses(course.getTotalClasses())
                .teacherId(
                        course.getTeacher() != null
                                ? course.getTeacher().getId()
                                : null
                )
                .teacherName(
                        course.getTeacher() != null
                                ? course.getTeacher()
                                        .getUser()
                                        .getFullName()
                                : null
                )
                .build();
    }
}