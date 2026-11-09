
package com.smartattendance.service.impl;

import com.smartattendance.attendance.entity.AttendanceSession;
import com.smartattendance.attendance.repository.AttendanceSessionRepository;
import com.smartattendance.dto.request.AttendanceRequest;
import com.smartattendance.dto.response.*;
import com.smartattendance.entity.*;
import com.smartattendance.enums.AttendanceSessionStatus;
import com.smartattendance.enums.AttendanceStatus;
import com.smartattendance.repository.*;
import com.smartattendance.service.AttendanceService;
import com.smartattendance.service.FaceRecognitionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final FaceRecognitionService faceRecognitionService;
    private final TeacherRepository teacherRepository;

    // ============================================================
    // COMMON HELPERS
    // ============================================================

    private Teacher findTeacherByEmail(String email) {
        return teacherRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Teacher account not found."
                ));
    }

    private Student findStudentByEmail(String email) {
        return studentRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student account not found."
                ));
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Course not found."
                ));
    }

    private AttendanceSession findSession(Long sessionId) {
        return attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Attendance session not found."
                ));
    }

    private Attendance findAttendance(Long id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Attendance record not found."
                ));
    }

    private void verifyTeacherOwnsCourse(Long courseId, String email) {

        Teacher teacher = findTeacherByEmail(email);
        Course course = findCourse(courseId);

        if (course.getTeacher() == null ||
                !course.getTeacher().getId().equals(teacher.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to access this course."
            );
        }
    }

    private void verifyTeacherOwnsSession(
            AttendanceSession session,
            String email) {

        if (session.getTeacher() == null ||
                session.getTeacher().getUser() == null ||
                !session.getTeacher().getUser().getEmail()
                        .equalsIgnoreCase(email)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to access this attendance session."
            );
        }
    }

    private void verifyStudentOwnsRecord(Long studentId, String email) {

        Student student = findStudentByEmail(email);

        if (!student.getId().equals(studentId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can access only your own attendance records."
            );
        }
    }

    private void validateThreshold(double threshold) {

        if (!Double.isFinite(threshold) ||
                threshold < 0 || threshold > 100) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Attendance threshold must be between 0 and 100."
            );
        }
    }

    private void verifyStudentOrTeacherAccess(
            Long studentId,
            String email,
            boolean isAdmin,
            boolean isStudent,
            List<Enrollment> enrollments) {

        if (isAdmin) {
            return;
        }

        if (isStudent) {
            verifyStudentOwnsRecord(studentId, email);
            return;
        }

        Teacher teacher = findTeacherByEmail(email);

        enrollments.removeIf(enrollment ->
                enrollment.getCourse() == null ||
                enrollment.getCourse().getTeacher() == null ||
                !enrollment.getCourse().getTeacher()
                        .getId().equals(teacher.getId())
        );
    }

    private void verifyStudentTeacherOrAdminAccess(
            Long studentId,
            String email,
            boolean isAdmin,
            boolean isStudent) {

        if (isAdmin) {
            return;
        }

        if (isStudent) {
            verifyStudentOwnsRecord(studentId, email);
            return;
        }

        Teacher teacher = findTeacherByEmail(email);

        boolean hasAssignedCourse = enrollmentRepository
                .findByStudentId(studentId)
                .stream()
                .anyMatch(enrollment ->
                        enrollment.getCourse() != null &&
                        enrollment.getCourse().getTeacher() != null &&
                        enrollment.getCourse().getTeacher()
                                .getId().equals(teacher.getId())
                );

        if (!hasAssignedCourse) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to access this student's attendance."
            );
        }
    }

    // ============================================================
    // MANUAL ATTENDANCE
    // ============================================================

    @Override
    public AttendanceResponse markAttendance(
            AttendanceRequest request,
            String email,
            boolean isAdmin) {

        if (request == null ||
                request.getStudentId() == null ||
                request.getSessionId() == null ||
                request.getStatus() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Student ID, session ID and attendance status are required."
            );
        }

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found."
                ));

        AttendanceSession session = findSession(request.getSessionId());

        if (session.getStatus() != AttendanceSessionStatus.OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Attendance session is closed."
            );
        }

        if (!isAdmin) {
            verifyTeacherOwnsSession(session, email);
        }

        Course course = session.getCourse();

        if (course == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No course is associated with this session."
            );
        }

        boolean enrolled =
                enrollmentRepository.existsByStudentIdAndCourseId(
                        student.getId(),
                        course.getId()
                );

        if (!enrolled) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Student is not enrolled in this course."
            );
        }

        if (attendanceRepository.existsByStudentIdAndSessionId(
                student.getId(),
                session.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attendance has already been marked."
            );
        }

        Attendance attendance = Attendance.builder()
                .student(student)
                .course(course)
                .session(session)
                .attendanceDate(session.getAttendanceDate())
                .status(request.getStatus())
                .build();

        return mapToResponse(attendanceRepository.save(attendance));
    }

    // ============================================================
    // ATTENDANCE SUMMARY
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getAttendanceSummary(
            Long studentId,
            String email,
            boolean isAdmin,
            boolean isStudent) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found."
                ));

        List<Enrollment> enrollments =
                new ArrayList<>(
                        enrollmentRepository.findByStudentId(studentId)
                );

        verifyStudentOrTeacherAccess(
                studentId,
                email,
                isAdmin,
                isStudent,
                enrollments
        );

        long totalSessions = 0;
        long presentCount = 0;
        long absentCount = 0;

        for (Enrollment enrollment : enrollments) {

            Course course = enrollment.getCourse();

            if (course == null) {
                continue;
            }

            Long courseId = course.getId();

            totalSessions +=
                    attendanceSessionRepository.countByCourseIdAndStatus(
                            courseId,
                            AttendanceSessionStatus.CLOSED
                    );

            presentCount +=
                    attendanceRepository.countByStudentIdAndCourseIdAndStatus(
                            studentId,
                            courseId,
                            AttendanceStatus.PRESENT
                    );

            absentCount +=
                    attendanceRepository.countByStudentIdAndCourseIdAndStatus(
                            studentId,
                            courseId,
                            AttendanceStatus.ABSENT
                    );
        }

        double percentage = totalSessions > 0
                ? ((double) presentCount / totalSessions) * 100.0
                : 0.0;

        return AttendanceSummaryResponse.builder()
                .studentId(student.getId())
                .studentName(student.getUser().getFullName())
                .totalSessions(totalSessions)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .attendancePercentage(
                        Math.round(percentage * 100.0) / 100.0
                )
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getMyAttendanceSummary(String email) {

        Student student = findStudentByEmail(email);

        return getAttendanceSummary(
                student.getId(),
                email,
                false,
                true
        );
    }

    // ============================================================
    // MY COURSE-WISE ATTENDANCE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<CourseAttendanceSummaryResponse> getMyCourseWiseAttendance(
            String email) {

        Student student = findStudentByEmail(email);

        return getCourseWiseAttendance(
                student.getId(),
                email,
                false,
                true
        );
    }

    // ============================================================
    // COURSE-WISE ATTENDANCE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<CourseAttendanceSummaryResponse> getCourseWiseAttendance(
            Long studentId,
            String email,
            boolean isAdmin,
            boolean isStudent) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found."
                ));

        List<Enrollment> enrollments =
                new ArrayList<>(
                        enrollmentRepository.findByStudentId(studentId)
                );

        verifyStudentOrTeacherAccess(
                studentId,
                email,
                isAdmin,
                isStudent,
                enrollments
        );

        List<CourseAttendanceSummaryResponse> response =
                new ArrayList<>();

        for (Enrollment enrollment : enrollments) {

            Course course = enrollment.getCourse();

            if (course == null) {
                continue;
            }

            Long courseId = course.getId();

            long totalSessions =
                    attendanceRepository.countByStudentIdAndCourseId(
                            studentId,
                            courseId
                    );

            long presentCount =
                    attendanceRepository.countByStudentIdAndCourseIdAndStatus(
                            studentId,
                            courseId,
                            AttendanceStatus.PRESENT
                    );

            long absentCount =
                    attendanceRepository.countByStudentIdAndCourseIdAndStatus(
                            studentId,
                            courseId,
                            AttendanceStatus.ABSENT
                    );

            double percentage = totalSessions > 0
                    ? ((double) presentCount / totalSessions) * 100.0
                    : 0.0;

            response.add(CourseAttendanceSummaryResponse.builder()
                    .courseId(courseId)
                    .courseCode(course.getCourseCode())
                    .courseName(course.getCourseName())
                    .totalSessions(totalSessions)
                    .presentCount(presentCount)
                    .absentCount(absentCount)
                    .attendancePercentage(
                            Math.round(percentage * 100.0) / 100.0
                    )
                    .build());
        }

        return response;
    }

    // ============================================================
    // ATTENDANCE BY STUDENT
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByStudent(
            Long studentId,
            String email,
            boolean isAdmin,
            boolean isStudent) {

        if (!studentRepository.existsById(studentId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Student not found."
            );
        }

        verifyStudentTeacherOrAdminAccess(
                studentId,
                email,
                isAdmin,
                isStudent
        );

        List<Attendance> records =
                attendanceRepository.findByStudentId(studentId);

        if (isAdmin || isStudent) {
            return records.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        Teacher teacher = findTeacherByEmail(email);

        return records.stream()
                .filter(record ->
                        record.getCourse() != null &&
                        record.getCourse().getTeacher() != null &&
                        record.getCourse().getTeacher()
                                .getId().equals(teacher.getId()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // ATTENDANCE BY COURSE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByCourse(
            Long courseId,
            String email,
            boolean isAdmin) {

        if (!isAdmin) {
            verifyTeacherOwnsCourse(courseId, email);
        } else {
            findCourse(courseId);
        }

        return attendanceRepository.findByCourseId(courseId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // LOW ATTENDANCE STUDENTS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<LowAttendanceResponse> getLowAttendanceStudents(
            Long courseId,
            double threshold,
            String email,
            boolean isAdmin) {

        validateThreshold(threshold);

        if (!isAdmin) {
            verifyTeacherOwnsCourse(courseId, email);
        }

        Course course = findCourse(courseId);

        List<Enrollment> enrollments =
                enrollmentRepository.findByCourseId(courseId);

        long totalSessions =
                attendanceSessionRepository.countByCourseIdAndStatus(
                        courseId,
                        AttendanceSessionStatus.CLOSED
                );

        List<LowAttendanceResponse> response =
                new ArrayList<>();

        for (Enrollment enrollment : enrollments) {

            Student student = enrollment.getStudent();

            if (student == null) {
                continue;
            }

            Long studentId = student.getId();

            long presentCount =
                    attendanceRepository.countByStudentIdAndCourseIdAndStatus(
                            studentId,
                            courseId,
                            AttendanceStatus.PRESENT
                    );

            long absentCount =
                    attendanceRepository.countByStudentIdAndCourseIdAndStatus(
                            studentId,
                            courseId,
                            AttendanceStatus.ABSENT
                    );

            double percentage = totalSessions > 0
                    ? ((double) presentCount / totalSessions) * 100.0
                    : 0.0;

            if (percentage < threshold) {

                double shortage = threshold - percentage;

                response.add(LowAttendanceResponse.builder()
                        .studentId(studentId)
                        .studentName(student.getUser().getFullName())
                        .registrationNumber(student.getRegistrationNumber())
                        .courseId(course.getId())
                        .courseCode(course.getCourseCode())
                        .courseName(course.getCourseName())
                        .totalSessions(totalSessions)
                        .presentCount(presentCount)
                        .absentCount(absentCount)
                        .attendancePercentage(
                                Math.round(percentage * 100.0) / 100.0
                        )
                        .requiredPercentage(threshold)
                        .shortagePercentage(
                                Math.round(shortage * 100.0) / 100.0
                        )
                        .build());
            }
        }

        return response;
    }

    // ============================================================
    // STUDENT COURSE ATTENDANCE REPORT
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<StudentCourseAttendanceResponse>
    getStudentCourseAttendanceReport(
            Long courseId,
            double threshold,
            String email,
            boolean isAdmin) {

        validateThreshold(threshold);

        if (!isAdmin) {
            verifyTeacherOwnsCourse(courseId, email);
        }

        Course course = findCourse(courseId);

        List<Enrollment> enrollments =
                enrollmentRepository.findByCourseId(courseId);

        long totalSessions =
                attendanceSessionRepository.countByCourseIdAndStatus(
                        courseId,
                        AttendanceSessionStatus.CLOSED
                );

        List<StudentCourseAttendanceResponse> response =
                new ArrayList<>();

        for (Enrollment enrollment : enrollments) {

            Student student = enrollment.getStudent();

            if (student == null) {
                continue;
            }

            Long studentId = student.getId();

            long presentCount =
                    attendanceRepository.countByStudentIdAndCourseIdAndStatus(
                            studentId,
                            courseId,
                            AttendanceStatus.PRESENT
                    );

            long absentCount =
                    attendanceRepository.countByStudentIdAndCourseIdAndStatus(
                            studentId,
                            courseId,
                            AttendanceStatus.ABSENT
                    );

            double percentage = totalSessions > 0
                    ? ((double) presentCount / totalSessions) * 100.0
                    : 0.0;

            String status = percentage < threshold
                    ? "LOW_ATTENDANCE"
                    : "GOOD";

            response.add(StudentCourseAttendanceResponse.builder()
                    .studentId(studentId)
                    .studentName(student.getUser().getFullName())
                    .registrationNumber(student.getRegistrationNumber())
                    .courseId(course.getId())
                    .courseCode(course.getCourseCode())
                    .courseName(course.getCourseName())
                    .totalSessions(totalSessions)
                    .presentCount(presentCount)
                    .absentCount(absentCount)
                    .attendancePercentage(
                            Math.round(percentage * 100.0) / 100.0
                    )
                    .requiredPercentage(threshold)
                    .status(status)
                    .build());
        }

        return response;
    }

    // ============================================================
    // FACE REGISTRATION
    // ============================================================

    @Override
    public boolean registerFace(Long studentId, MultipartFile image) {

        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Face image is required."
            );
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found."
                ));

        boolean registered = faceRecognitionService.registerFace(
                student.getRegistrationNumber(),
                image
        );

        if (!registered) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Face registration failed."
            );
        }

        student.setFaceRegistered(true);
        studentRepository.save(student);

        return true;
    }

    // ============================================================
    // COURSE ATTENDANCE ANALYTICS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public CourseAttendanceAnalyticsResponse getCourseAttendanceAnalytics(
            Long courseId,
            double threshold,
            String email,
            boolean isAdmin) {

        validateThreshold(threshold);

        if (!isAdmin) {
            verifyTeacherOwnsCourse(courseId, email);
        }

        Course course = findCourse(courseId);

        long totalStudents =
                enrollmentRepository.countByCourseId(courseId);

        long totalSessions =
                attendanceSessionRepository.countByCourseIdAndStatus(
                        courseId,
                        AttendanceSessionStatus.CLOSED
                );

        long totalPresent =
                attendanceRepository.countByCourseIdAndStatus(
                        courseId,
                        AttendanceStatus.PRESENT
                );

        long totalAbsent =
                attendanceRepository.countByCourseIdAndStatus(
                        courseId,
                        AttendanceStatus.ABSENT
                );

        double percentage = 0.0;

        if (totalStudents > 0 && totalSessions > 0) {
            long expected = totalStudents * totalSessions;
            percentage = ((double) totalPresent / expected) * 100.0;
        }

        List<LowAttendanceResponse> lowAttendance =
                getLowAttendanceStudents(
                        courseId,
                        threshold,
                        email,
                        isAdmin
                );

        return CourseAttendanceAnalyticsResponse.builder()
                .courseId(course.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .totalStudents(totalStudents)
                .totalSessions(totalSessions)
                .totalPresent(totalPresent)
                .totalAbsent(totalAbsent)
                .averageAttendancePercentage(
                        Math.round(percentage * 100.0) / 100.0
                )
                .lowAttendanceStudents(lowAttendance.size())
                .requiredPercentage(threshold)
                .build();
    }

    // ============================================================
    // ATTENDANCE BY DATE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByDate(
            LocalDate date,
            String email,
            boolean isAdmin) {

        if (date == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Attendance date is required."
            );
        }

        List<Attendance> records =
                attendanceRepository.findByAttendanceDate(date);

        if (isAdmin) {
            return records.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        Teacher teacher = findTeacherByEmail(email);

        return records.stream()
                .filter(record ->
                        record.getCourse() != null &&
                        record.getCourse().getTeacher() != null &&
                        record.getCourse().getTeacher()
                                .getId().equals(teacher.getId()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================================
    // GET ATTENDANCE BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getAttendance(Long id) {

        Attendance attendance = findAttendance(id);

        return mapToResponse(attendance);
    }

    // ============================================================
    // UPDATE ATTENDANCE
    // ============================================================

    @Override
    public AttendanceResponse updateAttendance(
            Long id,
            AttendanceRequest request,
            String email,
            boolean isAdmin) {

        Attendance attendance = findAttendance(id);

        AttendanceSession session = attendance.getSession();

        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Attendance session not found."
            );
        }

        if (!isAdmin) {
            verifyTeacherOwnsSession(session, email);
        }

        if (session.getStatus() != AttendanceSessionStatus.OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Attendance session is closed."
            );
        }

        if (request == null || request.getStatus() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Attendance status is required."
            );
        }

        attendance.setStatus(request.getStatus());

        return mapToResponse(attendanceRepository.save(attendance));
    }

    // ============================================================
    // DELETE ATTENDANCE
    // ============================================================

    @Override
    public void deleteAttendance(Long id) {

        Attendance attendance = findAttendance(id);

        AttendanceSession session = attendance.getSession();

        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Attendance session not found."
            );
        }

        if (session.getStatus() != AttendanceSessionStatus.OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Attendance session is closed."
            );
        }

        attendanceRepository.delete(attendance);
    }

    // ============================================================
    // FACE-RECOGNITION ATTENDANCE
    // ============================================================

    @Override
    public AttendanceResponse markAttendanceByFace(
            Long sessionId,
            String recognizedRegistrationNumber,
            String loggedInEmail) {

        Student loggedInStudent = findStudentByEmail(loggedInEmail);

        if (recognizedRegistrationNumber == null ||
                recognizedRegistrationNumber.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Face recognition did not return a student registration number."
            );
        }

        Student recognizedStudent =
                studentRepository.findByRegistrationNumber(
                        recognizedRegistrationNumber
                ).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Recognized student not found."
                ));

        if (!loggedInStudent.getId().equals(recognizedStudent.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Face does not match the logged-in student."
            );
        }

        if (!Boolean.TRUE.equals(recognizedStudent.getFaceRegistered())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Face is not registered for this student."
            );
        }

        AttendanceSession session = findSession(sessionId);

        if (session.getStatus() != AttendanceSessionStatus.OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Attendance session is closed."
            );
        }

        Course course = session.getCourse();

        if (course == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No course is associated with this session."
            );
        }

        boolean enrolled =
                enrollmentRepository.existsByStudentIdAndCourseId(
                        loggedInStudent.getId(),
                        course.getId()
                );

        if (!enrolled) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Student is not enrolled in this course."
            );
        }

        if (attendanceRepository.existsByStudentIdAndSessionId(
                loggedInStudent.getId(),
                sessionId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attendance has already been marked."
            );
        }

        Attendance attendance = Attendance.builder()
                .student(loggedInStudent)
                .course(course)
                .session(session)
                .attendanceDate(session.getAttendanceDate())
                .status(AttendanceStatus.PRESENT)
                .build();

        return mapToResponse(attendanceRepository.save(attendance));
    }

    // ============================================================
    // ADMIN ATTENDANCE DASHBOARD
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public AdminAttendanceDashboardResponse getAdminAttendanceDashboard() {

        long totalSessions = attendanceSessionRepository.count();

        long openSessions =
                attendanceSessionRepository.countByStatus(
                        AttendanceSessionStatus.OPEN
                );

        long closedSessions =
                attendanceSessionRepository.countByStatus(
                        AttendanceSessionStatus.CLOSED
                );

        long totalAttendanceRecords = attendanceRepository.count();

        long presentCount =
                attendanceRepository.countByStatus(
                        AttendanceStatus.PRESENT
                );

        long absentCount =
                attendanceRepository.countByStatus(
                        AttendanceStatus.ABSENT
                );

        long markedRecords = presentCount + absentCount;

        double percentage = 0.0;

        if (markedRecords > 0) {
            percentage =
                    ((double) presentCount / markedRecords) * 100.0;

            percentage = Math.round(percentage * 100.0) / 100.0;
        }

        return AdminAttendanceDashboardResponse.builder()
                .totalSessions(totalSessions)
                .openSessions(openSessions)
                .closedSessions(closedSessions)
                .totalAttendanceRecords(totalAttendanceRecords)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .attendancePercentage(percentage)
                .build();
    }

    // ============================================================
    // MAP ENTITY TO RESPONSE
    // ============================================================

    private AttendanceResponse mapToResponse(Attendance attendance) {

        return AttendanceResponse.builder()
                .id(attendance.getId())
                .studentName(
                        attendance.getStudent()
                                .getUser()
                                .getFullName()
                )
                .courseName(
                        attendance.getCourse()
                                .getCourseName()
                )
                .sessionId(
                        attendance.getSession()
                                .getId()
                )
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus())
                .build();
    }
}
