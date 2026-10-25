package com.smartattendance.attendance.service;

import com.smartattendance.attendance.dto.request.StartAttendanceRequest;
import com.smartattendance.attendance.dto.response.AttendanceSessionHistoryResponse;
import com.smartattendance.attendance.dto.response.AttendanceSessionResponse;
import com.smartattendance.attendance.dto.response.AttendanceSessionStudentResponse;
import com.smartattendance.attendance.entity.AttendanceSession;
import com.smartattendance.attendance.repository.AttendanceSessionRepository;

import com.smartattendance.entity.Attendance;
import com.smartattendance.entity.Course;
import com.smartattendance.entity.Enrollment;
import com.smartattendance.entity.Student;
import com.smartattendance.entity.Teacher;

import com.smartattendance.enums.AttendanceSessionStatus;
import com.smartattendance.enums.AttendanceStatus;

import com.smartattendance.repository.AttendanceRepository;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.EnrollmentRepository;
import com.smartattendance.repository.StudentRepository;
import com.smartattendance.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceSessionServiceImpl
        implements AttendanceSessionService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;


    // ============================================================
    // START ATTENDANCE SESSION
    // ============================================================

    @Override
    public AttendanceSessionResponse startSession(
            StartAttendanceRequest request) {

        // --------------------------------------------------------
        // 1. Get logged-in authentication
        // --------------------------------------------------------

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }


        // --------------------------------------------------------
        // 2. Get logged-in teacher email from JWT
        // --------------------------------------------------------

        String email = authentication.getName();

        Teacher teacher =
                teacherRepository
                        .findByUser_Email(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Teacher not found for logged-in user"
                                )
                        );


        // --------------------------------------------------------
        // 3. Validate course ID
        // --------------------------------------------------------

        if (request.getCourseId() == null) {

            throw new RuntimeException(
                    "Course ID is required"
            );
        }


        // --------------------------------------------------------
        // 4. Find course
        // --------------------------------------------------------

        Course course =
                courseRepository
                        .findById(request.getCourseId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Course not found with ID: "
                                                + request.getCourseId()
                                )
                        );


        // --------------------------------------------------------
        // 5. Verify teacher owns this course
        // --------------------------------------------------------

        if (course.getTeacher() == null ||
                !course.getTeacher()
                        .getId()
                        .equals(teacher.getId())) {

            throw new RuntimeException(
                    "You are not authorized to take attendance "
                            + "for this course"
            );
        }


        // --------------------------------------------------------
        // 6. Prevent multiple OPEN sessions for same course
        // --------------------------------------------------------

        boolean activeSession =
                attendanceSessionRepository
                        .existsByCourseIdAndStatus(
                                course.getId(),
                                AttendanceSessionStatus.OPEN
                        );

        if (activeSession) {

            throw new RuntimeException(
                    "An attendance session is already open "
                            + "for this course"
            );
        }


        // --------------------------------------------------------
        // 7. Create attendance session
        // --------------------------------------------------------

        AttendanceSession session =
                AttendanceSession.builder()
                        .teacher(teacher)
                        .course(course)
                        .attendanceDate(
                                LocalDate.now()
                        )
                        .startedAt(
                                LocalDateTime.now()
                        )
                        .status(
                                AttendanceSessionStatus.OPEN
                        )
                        .build();


        // --------------------------------------------------------
        // 8. Save session
        // --------------------------------------------------------

        AttendanceSession saved =
                attendanceSessionRepository.save(session);


        // --------------------------------------------------------
        // 9. Return response
        // --------------------------------------------------------

        return mapToResponse(saved);
    }


    // ============================================================
    // GET SESSION
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public AttendanceSessionResponse getSession(
            Long id) {

        AttendanceSession session =
                attendanceSessionRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Attendance session not found with ID: "
                                                + id
                                )
                        );

        return mapToResponse(session);
    }


    // ============================================================
    // GET OPEN SESSION FOR TEACHER'S COURSE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public AttendanceSessionResponse getOpenSessionForCourse(
            Long courseId) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }

        String email = authentication.getName();

        Teacher teacher =
                teacherRepository
                        .findByUser_Email(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Teacher not found for logged-in user"
                                )
                        );

        AttendanceSession session =
                attendanceSessionRepository
                        .findByCourseIdAndStatus(
                                courseId,
                                AttendanceSessionStatus.OPEN
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No open attendance session found for this course"
                                )
                        );

        if (session.getTeacher() == null ||
                !session.getTeacher()
                        .getId()
                        .equals(teacher.getId())) {

            throw new RuntimeException(
                    "You are not authorized to view this attendance session"
            );
        }

        return mapToResponse(session);
    }


    // ============================================================
    // GET MY OPEN SESSIONS FOR STUDENT
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSessionResponse> getMyOpenSessions(
            String email) {

        Student student =
                studentRepository
                        .findByUser_Email(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found for logged-in user."
                                )
                        );

        List<Enrollment> enrollments =
                enrollmentRepository
                        .findByStudentId(student.getId());

        return enrollments.stream()
                .map(Enrollment::getCourse)
                .map(course ->
                        attendanceSessionRepository
                                .findByCourseIdAndStatus(
                                        course.getId(),
                                        AttendanceSessionStatus.OPEN
                                )
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::mapToResponse)
                .toList();
    }


    // ============================================================
    // CLOSE ATTENDANCE SESSION
    // ============================================================

    @Override
    public void closeSession(Long id) {

        // --------------------------------------------------------
        // 1. Get logged-in authentication
        // --------------------------------------------------------

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }


        // --------------------------------------------------------
        // 2. Get logged-in teacher
        // --------------------------------------------------------

        String email = authentication.getName();

        Teacher teacher =
                teacherRepository
                        .findByUser_Email(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Teacher not found for logged-in user"
                                )
                        );


        // --------------------------------------------------------
        // 3. Find attendance session
        // --------------------------------------------------------

        AttendanceSession session =
                attendanceSessionRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Attendance session not found with ID: "
                                                + id
                                )
                        );


        // --------------------------------------------------------
        // 4. Verify session belongs to teacher
        // --------------------------------------------------------

        if (session.getTeacher() == null ||
                !session.getTeacher()
                        .getId()
                        .equals(teacher.getId())) {

            throw new RuntimeException(
                    "You are not authorized to close this attendance session"
            );
        }


        // --------------------------------------------------------
        // 5. Session must be OPEN
        // --------------------------------------------------------

        if (session.getStatus()
                != AttendanceSessionStatus.OPEN) {

            throw new RuntimeException(
                    "Attendance session is already closed"
            );
        }


        // --------------------------------------------------------
        // 6. Get course
        // --------------------------------------------------------

        Course course = session.getCourse();

        if (course == null) {

            throw new RuntimeException(
                    "Course is not associated with this session"
            );
        }


        // --------------------------------------------------------
        // 7. Get all enrolled students
        // --------------------------------------------------------

        List<Enrollment> enrollments =
                enrollmentRepository
                        .findByCourseId(course.getId());


        // --------------------------------------------------------
        // 8. Find students who already marked attendance
        // --------------------------------------------------------

        List<Attendance> existingAttendance =
                attendanceRepository
                        .findBySessionId(session.getId());


        // --------------------------------------------------------
        // 9. Mark missing students as ABSENT
        // --------------------------------------------------------

        for (Enrollment enrollment : enrollments) {

            Student student =
                    enrollment.getStudent();

            boolean alreadyMarked =
                    existingAttendance
                            .stream()
                            .anyMatch(attendance ->
                                    attendance.getStudent()
                                            .getId()
                                            .equals(student.getId())
                            );

            if (!alreadyMarked) {

                Attendance absentAttendance =
                        Attendance.builder()
                                .student(student)
                                .course(course)
                                .session(session)
                                .attendanceDate(
                                        session.getAttendanceDate()
                                )
                                .status(
                                        AttendanceStatus.ABSENT
                                )
                                .build();

                attendanceRepository.save(
                        absentAttendance
                );
            }
        }


        // --------------------------------------------------------
        // 10. Close session
        // --------------------------------------------------------

        session.setStatus(
                AttendanceSessionStatus.CLOSED
        );

        session.setClosedAt(
                LocalDateTime.now()
        );


        // --------------------------------------------------------
        // 11. Save session
        // --------------------------------------------------------

        attendanceSessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSessionHistoryResponse> getMyHistory(
            String email) {

        // --------------------------------------------------------
        // 1. Find logged-in teacher
        // --------------------------------------------------------

        Teacher teacher =
                teacherRepository
                        .findByUser_Email(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Teacher not found for logged-in user."
                                )
                        );


        // --------------------------------------------------------
        // 2. Get all sessions belonging to this teacher
        // --------------------------------------------------------

        List<AttendanceSession> sessions =
                attendanceSessionRepository
                        .findByTeacherIdOrderByStartedAtDesc(
                                teacher.getId()
                        );


        // --------------------------------------------------------
        // 3. Convert sessions into history response
        // --------------------------------------------------------

        return sessions.stream()
                .map(session -> {

                    Course course = session.getCourse();

                    if (course == null) {
                        throw new RuntimeException(
                                "Course not found for attendance session "
                                        + session.getId()
                        );
                    }

                    List<Attendance> attendanceRecords =
                            attendanceRepository
                                    .findBySessionId(
                                            session.getId()
                                    );

                    long presentCount =
                            attendanceRecords.stream()
                                    .filter(attendance ->
                                            attendance.getStatus()
                                                    == AttendanceStatus.PRESENT
                                    )
                                    .count();

                    long absentCount =
                            attendanceRecords.stream()
                                    .filter(attendance ->
                                            attendance.getStatus()
                                                    == AttendanceStatus.ABSENT
                                    )
                                    .count();

                    long totalStudents =
                            enrollmentRepository
                                    .findByCourseId(course.getId())
                                    .size();

                    return AttendanceSessionHistoryResponse
                            .builder()
                            .sessionId(session.getId())
                            .courseId(course.getId())
                            .courseCode(course.getCourseCode())
                            .courseName(course.getCourseName())
                            .attendanceDate(
                                    session.getAttendanceDate()
                            )
                            .startedAt(
                                    session.getStartedAt()
                            )
                            .closedAt(
                                    session.getClosedAt()
                            )
                            .status(
                                    session.getStatus()
                            )
                            .totalStudents(totalStudents)
                            .presentCount(presentCount)
                            .absentCount(absentCount)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSessionStudentResponse> getSessionAttendance(
            Long sessionId,
            String email) {

        // 1. Find the session
        AttendanceSession session =
                attendanceSessionRepository
                        .findById(sessionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Attendance session not found with ID: "
                                                + sessionId
                                )
                        );

        // 2. Find logged-in teacher
        Teacher teacher =
                teacherRepository
                        .findByUser_Email(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Teacher not found for logged-in user."
                                )
                        );

        // 3. Verify that this session belongs to the teacher
        if (session.getTeacher() == null ||
                !session.getTeacher()
                        .getId()
                        .equals(teacher.getId())) {

            throw new RuntimeException(
                    "You are not authorized to view this attendance session."
            );
        }

        // 4. Get attendance records for this session
        List<Attendance> attendanceRecords =
                attendanceRepository
                        .findBySessionId(sessionId);

        // 5. Convert to response
        return attendanceRecords
                .stream()
                .map(attendance -> {

                    Student student =
                            attendance.getStudent();

                    return AttendanceSessionStudentResponse
                            .builder()
                            .studentId(student.getId())
                            .studentName(
                                    student.getUser().getFullName()
                            )
                            .registrationNumber(
                                    student.getRegistrationNumber()
                            )
                            .status(
                                    attendance.getStatus()
                            )
                            .build();
                })
                .toList();
    }


    // ============================================================
    // MAP ENTITY → RESPONSE
    // ============================================================

    private AttendanceSessionResponse mapToResponse(
            AttendanceSession session) {

        return AttendanceSessionResponse.builder()

                .id(
                        session.getId()
                )

                .courseId(
                        session.getCourse().getId()
                )

                .teacherId(
                        session.getTeacher().getId()
                )

                .attendanceDate(
                        session.getAttendanceDate()
                )

                .startedAt(
                        session.getStartedAt()
                )

                .closedAt(
                        session.getClosedAt()
                )

                .status(
                        session.getStatus()
                )

                .build();
    }
}