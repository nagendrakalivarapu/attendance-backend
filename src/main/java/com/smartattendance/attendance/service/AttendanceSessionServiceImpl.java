package com.smartattendance.attendance.service;

import com.smartattendance.attendance.dto.request.StartAttendanceRequest;
import com.smartattendance.attendance.dto.response.AttendanceSessionResponse;
import com.smartattendance.attendance.entity.AttendanceSession;
import com.smartattendance.attendance.repository.AttendanceSessionRepository;
import com.smartattendance.entity.*;
import com.smartattendance.enums.AttendanceSessionStatus;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import com.smartattendance.enums.AttendanceStatus;
import com.smartattendance.repository.AttendanceRepository;
import com.smartattendance.repository.EnrollmentRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartattendance.enums.AttendanceSessionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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