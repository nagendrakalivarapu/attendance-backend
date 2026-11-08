package com.smartattendance.service.impl;

import com.smartattendance.dto.request.CourseRequest;
import com.smartattendance.dto.response.CourseResponse;
import com.smartattendance.entity.Course;
import com.smartattendance.entity.Teacher;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.TeacherRepository;
import com.smartattendance.service.CourseService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;

    @Override
    public CourseResponse createCourse(CourseRequest request) {

        if (courseRepository.existsByCourseCode(request.getCourseCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Course code already exists.");
        }

        Teacher teacher = null;

        if (request.getTeacherId() != null) {
            teacher = teacherRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Teacher not found."));
        }

        Course course = Course.builder()
                .courseCode(request.getCourseCode().trim())
                .courseName(request.getCourseName().trim())
                .department(request.getDepartment().trim())
                .semester(request.getSemester())
                .credits(request.getCredits())
                .totalClasses(request.getTotalClasses())
                .teacher(teacher)
                .build();

        return mapToResponse(courseRepository.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(
            Long id, String email, boolean isAdmin) {

        Course course = findCourse(id);

        if (!isAdmin) {
            verifyTeacherOwnsCourse(course, email);
        }

        return mapToResponse(course);
    }

    @Override
    public CourseResponse updateCourse(
            Long id, CourseRequest request) {

        Course course = findCourse(id);

        if (courseRepository.existsByCourseCodeAndIdNot(
                request.getCourseCode().trim(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Course code already exists.");
        }

        Teacher teacher = null;

        if (request.getTeacherId() != null) {
            teacher = teacherRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Teacher not found."));
        }

        course.setCourseCode(request.getCourseCode().trim());
        course.setCourseName(request.getCourseName().trim());
        course.setDepartment(request.getDepartment().trim());
        course.setSemester(request.getSemester());
        course.setCredits(request.getCredits());
        course.setTotalClasses(request.getTotalClasses());
        course.setTeacher(teacher);

        return mapToResponse(courseRepository.save(course));
    }

    @Override
    public void deleteCourse(Long id) {
        Course course = findCourse(id);
        courseRepository.delete(course);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByDepartment(
            String department, String email, boolean isAdmin) {

        List<Course> courses = courseRepository.findByDepartment(department);

        return filterForCaller(courses, email, isAdmin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesBySemester(
            Integer semester, String email, boolean isAdmin) {

        List<Course> courses = courseRepository.findBySemester(semester);

        return filterForCaller(courses, email, isAdmin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByTeacher(Long teacherId) {
        return courseRepository.findByTeacherId(teacherId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getMyCourses(String email) {

        Teacher teacher = findTeacherByEmail(email);

        return courseRepository.findByTeacherId(teacher.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private List<CourseResponse> filterForCaller(
            List<Course> courses, String email, boolean isAdmin) {

        return courses.stream()
                .filter(course -> isAdmin ||
                        (course.getTeacher() != null &&
                         course.getTeacher().getUser().getEmail()
                                 .equalsIgnoreCase(email)))
                .map(this::mapToResponse)
                .toList();
    }

    private void verifyTeacherOwnsCourse(Course course, String email) {

        if (course.getTeacher() == null ||
                !course.getTeacher().getUser().getEmail()
                        .equalsIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not assigned to this course.");
        }
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Course not found."));
    }

    private Teacher findTeacherByEmail(String email) {
        return teacherRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Teacher not found for logged-in user."));
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
                .teacherId(course.getTeacher() != null
                        ? course.getTeacher().getId() : null)
                .teacherName(course.getTeacher() != null
                        ? course.getTeacher().getUser().getFullName() : null)
                .build();
    }
}