
package com.smartattendance.service.impl;

import com.smartattendance.dto.request.CourseRequest;
import com.smartattendance.dto.response.CourseResponse;
import com.smartattendance.entity.Course;
import com.smartattendance.entity.Teacher;
import com.smartattendance.entity.User;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.TeacherRepository;
import com.smartattendance.repository.EnrollmentRepository;
import com.smartattendance.attendance.repository.AttendanceSessionRepository;
import com.smartattendance.service.CourseService;

import java.util.List;
import java.util.Locale;

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
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;

    // ------------------------------------------------------------
    // CREATE COURSE
    // ------------------------------------------------------------

    @Override
    public CourseResponse createCourse(CourseRequest request) {

        validateRequest(request);

        String courseCode = normalizeRequired(
                request.getCourseCode(),
                "Course code is required.");

        if (courseRepository.existsByCourseCode(courseCode)) {
            throw conflict("Course code already exists.");
        }

        Teacher teacher = null;

        if (request.getTeacherId() != null) {
            teacher = findTeacher(request.getTeacherId());
            validateTeacherAccount(teacher);
        }

        Course course = Course.builder()
                .courseCode(courseCode)
                .courseName(normalizeRequired(
                        request.getCourseName(),
                        "Course name is required."))
                .department(normalizeRequired(
                        request.getDepartment(),
                        "Department is required."))
                .semester(request.getSemester())
                .credits(request.getCredits())
                .totalClasses(request.getTotalClasses())
                .teacher(teacher)
                .build();

        return mapToResponse(courseRepository.save(course));
    }

    // ------------------------------------------------------------
    // GET ALL COURSES
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {

        return courseRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ------------------------------------------------------------
    // GET COURSE BY ID
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(
            Long id,
            String email,
            boolean isAdmin) {

        Course course = findCourse(id);

        if (!isAdmin) {
            verifyTeacherOwnsCourse(course, email);
        }

        return mapToResponse(course);
    }

    // ------------------------------------------------------------
    // UPDATE COURSE
    // ------------------------------------------------------------

    @Override
    public CourseResponse updateCourse(
            Long id,
            CourseRequest request) {

        validateRequest(request);

        Course course = findCourse(id);

        String courseCode = normalizeRequired(
                request.getCourseCode(),
                "Course code is required.");

        if (!courseCode.equalsIgnoreCase(course.getCourseCode())
                && courseRepository.existsByCourseCodeAndIdNot(
                        courseCode, id)) {

            throw conflict("Course code already exists.");
        }

        Teacher teacher = null;

        if (request.getTeacherId() != null) {
            teacher = findTeacher(request.getTeacherId());
            validateTeacherAccount(teacher);
        }

        course.setCourseCode(courseCode);

        course.setCourseName(normalizeRequired(
                request.getCourseName(),
                "Course name is required."));

        course.setDepartment(normalizeRequired(
                request.getDepartment(),
                "Department is required."));

        course.setSemester(request.getSemester());
        course.setCredits(request.getCredits());
        course.setTotalClasses(request.getTotalClasses());
        course.setTeacher(teacher);

        return mapToResponse(courseRepository.save(course));
    }

    // ------------------------------------------------------------
    // DELETE COURSE
    // ------------------------------------------------------------

    @Override
    public void deleteCourse(Long id) {

        Course course = findCourse(id);

        // Preserve student enrollment history.
        if (enrollmentRepository.existsByCourseId(id)) {

            throw conflict(
                    "Cannot delete this course because student "
                            + "enrollments exist.");
        }

        // Preserve attendance session history.
        if (attendanceSessionRepository.existsByCourseId(id)) {

            throw conflict(
                    "Cannot delete this course because attendance "
                            + "sessions exist.");
        }

        courseRepository.delete(course);
    }

    // ------------------------------------------------------------
    // GET COURSES BY DEPARTMENT
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByDepartment(
            String department,
            String email,
            boolean isAdmin) {

        String normalizedDepartment = normalizeRequired(
                department,
                "Department is required.");

        List<Course> courses =
                courseRepository.findByDepartment(
                        normalizedDepartment);

        return filterForCaller(courses, email, isAdmin);
    }

    // ------------------------------------------------------------
    // GET COURSES BY SEMESTER
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesBySemester(
            Integer semester,
            String email,
            boolean isAdmin) {

        if (semester == null || semester < 1) {
            throw badRequest("Invalid semester.");
        }

        List<Course> courses =
                courseRepository.findBySemester(semester);

        return filterForCaller(courses, email, isAdmin);
    }

    // ------------------------------------------------------------
    // GET COURSES BY TEACHER
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByTeacher(Long teacherId) {

        if (teacherId == null) {
            throw badRequest("Teacher ID is required.");
        }

        findTeacher(teacherId);

        return courseRepository.findByTeacherId(teacherId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ------------------------------------------------------------
    // GET LOGGED-IN TEACHER'S COURSES
    // ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getMyCourses(String email) {

        Teacher teacher = findTeacherByEmail(email);

        return courseRepository.findByTeacherId(teacher.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ------------------------------------------------------------
    // FILTER COURSES FOR CALLER
    // ------------------------------------------------------------

    private List<CourseResponse> filterForCaller(
            List<Course> courses,
            String email,
            boolean isAdmin) {

        if (isAdmin) {
            return courses.stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        String normalizedEmail = normalizeEmail(email);

        return courses.stream()
                .filter(course ->
                        isCourseAssignedTo(course, normalizedEmail))
                .map(this::mapToResponse)
                .toList();
    }

    private boolean isCourseAssignedTo(
            Course course,
            String email) {

        if (course.getTeacher() == null
                || course.getTeacher().getUser() == null) {
            return false;
        }

        return normalizeEmail(
                course.getTeacher().getUser().getEmail()
        ).equals(email);
    }

    // ------------------------------------------------------------
    // VERIFY TEACHER OWNERSHIP
    // ------------------------------------------------------------

    private void verifyTeacherOwnsCourse(
            Course course,
            String email) {

        String normalizedEmail = normalizeEmail(email);

        if (!isCourseAssignedTo(course, normalizedEmail)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not assigned to this course.");
        }
    }

    // ------------------------------------------------------------
    // FIND COURSE
    // ------------------------------------------------------------

    private Course findCourse(Long id) {

        if (id == null) {
            throw badRequest("Course ID is required.");
        }

        return courseRepository.findById(id)
                .orElseThrow(() ->
                        notFound("Course not found."));
    }

    // ------------------------------------------------------------
    // FIND TEACHER
    // ------------------------------------------------------------

    private Teacher findTeacher(Long id) {

        return teacherRepository.findById(id)
                .orElseThrow(() ->
                        notFound("Teacher not found."));
    }

    private Teacher findTeacherByEmail(String email) {

        String normalizedEmail = normalizeEmail(email);

        Teacher teacher = teacherRepository
                .findByUser_Email(normalizedEmail)
                .orElseThrow(() ->
                        notFound(
                                "Teacher not found for logged-in user."));

        validateTeacherAccount(teacher);

        return teacher;
    }

    private void validateTeacherAccount(Teacher teacher) {

        if (teacher.getUser() == null) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Teacher has no linked user account.");
        }
    }

    // ------------------------------------------------------------
    // REQUEST VALIDATION
    // ------------------------------------------------------------

    private void validateRequest(CourseRequest request) {

        if (request == null) {
            throw badRequest("Course details are required.");
        }
    }

    private String normalizeRequired(
            String value,
            String message) {

        if (value == null || value.isBlank()) {
            throw badRequest(message);
        }

        return value.trim();
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw badRequest("Email is required.");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------
    // EXCEPTION HELPERS
    // ------------------------------------------------------------

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message);
    }

    // ------------------------------------------------------------
    // ENTITY TO RESPONSE
    // ------------------------------------------------------------

    private CourseResponse mapToResponse(Course course) {

        Teacher teacher = course.getTeacher();

        User user = teacher != null
                ? teacher.getUser()
                : null;

        return CourseResponse.builder()
                .id(course.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .department(course.getDepartment())
                .semester(course.getSemester())
                .credits(course.getCredits())
                .totalClasses(course.getTotalClasses())
                .teacherId(
                        teacher != null ? teacher.getId() : null)
                .teacherName(
                        user != null ? user.getFullName() : null)
                .build();
    }
}
