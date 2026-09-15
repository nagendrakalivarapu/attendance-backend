package com.smartattendance.service.impl;

import com.smartattendance.attendance.entity.AttendanceSession;
import com.smartattendance.attendance.repository.AttendanceSessionRepository;
import com.smartattendance.dto.request.AttendanceRequest;
import com.smartattendance.dto.response.*;
import com.smartattendance.entity.Attendance;
import com.smartattendance.entity.Course;
import com.smartattendance.entity.Enrollment;
import com.smartattendance.entity.Student;
import com.smartattendance.enums.AttendanceSessionStatus;
import com.smartattendance.enums.AttendanceStatus;
import com.smartattendance.repository.AttendanceRepository;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.EnrollmentRepository;
import com.smartattendance.repository.StudentRepository;
import com.smartattendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    // ============================================================
    // MARK ATTENDANCE
    // ============================================================

    @Override
    public AttendanceResponse markAttendance(
            AttendanceRequest request) {

        // --------------------------------------------------------
        // 1. Validate request
        // --------------------------------------------------------

        if (request.getStudentId() == null) {
            throw new RuntimeException(
                    "Student ID is required"
            );
        }

        if (request.getSessionId() == null) {
            throw new RuntimeException(
                    "Session ID is required"
            );
        }

        if (request.getStatus() == null) {
            throw new RuntimeException(
                    "Attendance status is required"
            );
        }


        // --------------------------------------------------------
        // 2. Find student
        // --------------------------------------------------------

        Student student =
                studentRepository
                        .findById(request.getStudentId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found with ID: "
                                                + request.getStudentId()
                                )
                        );


        // --------------------------------------------------------
        // 3. Find attendance session
        // --------------------------------------------------------

        AttendanceSession session =
                attendanceSessionRepository
                        .findById(request.getSessionId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Attendance session not found with ID: "
                                                + request.getSessionId()
                                )
                        );


        // --------------------------------------------------------
        // 4. Session must be OPEN
        // --------------------------------------------------------

        if (session.getStatus()
                != AttendanceSessionStatus.OPEN) {

            throw new RuntimeException(
                    "Attendance session is closed. "
                            + "Attendance cannot be marked."
            );
        }


        // --------------------------------------------------------
        // 5. Prevent duplicate attendance
        // --------------------------------------------------------

        boolean alreadyMarked =
                attendanceRepository
                        .existsByStudentIdAndSessionId(
                                request.getStudentId(),
                                request.getSessionId()
                        );

        if (alreadyMarked) {

            throw new RuntimeException(
                    "Attendance already marked for student ID "
                            + request.getStudentId()
                            + " in session ID "
                            + request.getSessionId()
            );
        }


        // --------------------------------------------------------
        // 6. Get course from session
        // --------------------------------------------------------

        if (session.getCourse() == null) {

            throw new RuntimeException(
                    "Course is not associated with this attendance session"
            );
        }

        var course = session.getCourse();


        // --------------------------------------------------------
        // 7. Verify student enrollment
        // --------------------------------------------------------

        boolean enrolled =
                enrollmentRepository.existsByStudentIdAndCourseId(
                        student.getId(),
                        course.getId()
                );

        if (!enrolled) {

            throw new RuntimeException(
                    "Student with ID "
                            + student.getId()
                            + " is not enrolled in course ID "
                            + course.getId()
            );
        }


        // --------------------------------------------------------
        // 8. Create attendance record
        // --------------------------------------------------------

        Attendance attendance =
                Attendance.builder()
                        .student(student)
                        .course(course)
                        .session(session)
                        .attendanceDate(
                                session.getAttendanceDate()
                        )
                        .status(request.getStatus())
                        .build();


        // --------------------------------------------------------
        // 9. Save attendance
        // --------------------------------------------------------

        Attendance savedAttendance =
                attendanceRepository.save(attendance);


        // --------------------------------------------------------
        // 10. Return response
        // --------------------------------------------------------

        return mapToResponse(savedAttendance);
    }
    //---------------------------------------------------
    //sum of attendance
    //----------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getAttendanceSummary(Long studentId) {

        Student student =
                studentRepository
                        .findById(studentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found with ID: " + studentId
                                )
                        );

        List<Enrollment> enrollments =
                enrollmentRepository.findByStudentId(studentId);

        long totalSessions = 0;
        long presentCount = 0;
        long absentCount = 0;

        for (Enrollment enrollment : enrollments) {

            Course course = enrollment.getCourse();

            if (course == null) {
                continue;
            }

            Long courseId = course.getId();

            long courseSessions =
                    attendanceSessionRepository
                            .countByCourseIdAndStatus(
                                    courseId,
                                    AttendanceSessionStatus.CLOSED
                            );

            long coursePresent =
                    attendanceRepository
                            .countByStudentIdAndCourseIdAndStatus(
                                    studentId,
                                    courseId,
                                    AttendanceStatus.PRESENT
                            );

            long courseAbsent =
                    attendanceRepository
                            .countByStudentIdAndCourseIdAndStatus(
                                    studentId,
                                    courseId,
                                    AttendanceStatus.ABSENT
                            );

            totalSessions += courseSessions;
            presentCount += coursePresent;
            absentCount += courseAbsent;
        }

        double attendancePercentage = 0.0;

        if (totalSessions > 0) {
            attendancePercentage =
                    ((double) presentCount / totalSessions) * 100.0;
        }

        return AttendanceSummaryResponse.builder()
                .studentId(student.getId())
                .studentName(student.getUser().getFullName())
                .totalSessions(totalSessions)
                .presentCount(presentCount)
                .absentCount(absentCount)
                .attendancePercentage(
                        Math.round(
                                attendancePercentage * 100.0
                        ) / 100.0
                )
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public List<StudentCourseAttendanceResponse>
    getStudentCourseAttendanceReport(
            Long courseId,
            double threshold) {

        if (threshold < 0 || threshold > 100) {
            throw new RuntimeException(
                    "Attendance threshold must be between 0 and 100"
            );
        }

        Course course =
                courseRepository.findById(courseId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Course not found with ID: " + courseId
                                )
                        );

        List<Enrollment> enrollments =
                enrollmentRepository.findByCourseId(courseId);

        long totalSessions =
                attendanceSessionRepository
                        .countByCourseIdAndStatus(
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
                    attendanceRepository
                            .countByStudentIdAndCourseIdAndStatus(
                                    studentId,
                                    courseId,
                                    AttendanceStatus.PRESENT
                            );

            long absentCount =
                    attendanceRepository
                            .countByStudentIdAndCourseIdAndStatus(
                                    studentId,
                                    courseId,
                                    AttendanceStatus.ABSENT
                            );

            double attendancePercentage = 0.0;

            if (totalSessions > 0) {
                attendancePercentage =
                        ((double) presentCount / totalSessions) * 100.0;
            }

            String status;

            if (attendancePercentage < threshold) {
                status = "LOW_ATTENDANCE";
            } else {
                status = "GOOD";
            }

            response.add(
                    StudentCourseAttendanceResponse.builder()
                            .studentId(studentId)
                            .studentName(
                                    student.getUser().getFullName()
                            )
                            .registrationNumber(
                                    student.getRegistrationNumber()
                            )
                            .courseId(course.getId())
                            .courseCode(course.getCourseCode())
                            .courseName(course.getCourseName())
                            .totalSessions(totalSessions)
                            .presentCount(presentCount)
                            .absentCount(absentCount)
                            .attendancePercentage(
                                    Math.round(
                                            attendancePercentage * 100.0
                                    ) / 100.0
                            )
                            .requiredPercentage(threshold)
                            .status(status)
                            .build()
            );
        }

        return response;
    }
    @Override
    @Transactional(readOnly = true)
    public List<CourseAttendanceSummaryResponse> getCourseWiseAttendance(
            Long studentId) {

        Student student =
                studentRepository
                        .findById(studentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found with ID: " + studentId
                                )
                        );

        List<Enrollment> enrollments =
                enrollmentRepository.findByStudentId(studentId);

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
                    attendanceRepository
                            .countByStudentIdAndCourseIdAndStatus(
                                    studentId,
                                    courseId,
                                    AttendanceStatus.PRESENT
                            );

            long absentCount =
                    attendanceRepository
                            .countByStudentIdAndCourseIdAndStatus(
                                    studentId,
                                    courseId,
                                    AttendanceStatus.ABSENT
                            );

            double attendancePercentage = 0.0;

            if (totalSessions > 0) {
                attendancePercentage =
                        ((double) presentCount / totalSessions) * 100.0;
            }

            response.add(
                    CourseAttendanceSummaryResponse.builder()
                            .courseId(courseId)
                            .courseCode(course.getCourseCode())
                            .courseName(course.getCourseName())
                            .totalSessions(totalSessions)
                            .presentCount(presentCount)
                            .absentCount(absentCount)
                            .attendancePercentage(
                                    Math.round(
                                            attendancePercentage * 100.0
                                    ) / 100.0
                            )
                            .build()
            );
        }

        return response;
    }
    // ============================================================
    // GET ATTENDANCE BY STUDENT
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByStudent(
            Long studentId) {

        if (!studentRepository.existsById(studentId)) {

            throw new RuntimeException(
                    "Student not found with ID: "
                            + studentId
            );
        }

        return attendanceRepository
                .findByStudentId(studentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // ============================================================
    // GET ATTENDANCE BY COURSE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByCourse(
            Long courseId) {

        return attendanceRepository
                .findByCourseId(courseId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LowAttendanceResponse> getLowAttendanceStudents(
            Long courseId,
            double threshold) {

        if (threshold < 0 || threshold > 100) {
            throw new RuntimeException(
                    "Attendance threshold must be between 0 and 100"
            );
        }

        Course course =
                courseRepository.findById(courseId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Course not found with ID: " + courseId
                                )
                        );

        List<Enrollment> enrollments =
                enrollmentRepository.findByCourseId(courseId);

        List<LowAttendanceResponse> response =
                new ArrayList<>();

        for (Enrollment enrollment : enrollments) {

            Student student = enrollment.getStudent();

            if (student == null) {
                continue;
            }

            Long studentId = student.getId();

            long totalSessions =
                    attendanceSessionRepository.countByCourseIdAndStatus(
                            courseId,
                            AttendanceSessionStatus.CLOSED
                    );

            long presentCount =
                    attendanceRepository
                            .countByStudentIdAndCourseIdAndStatus(
                                    studentId,
                                    courseId,
                                    AttendanceStatus.PRESENT
                            );

            long absentCount =
                    attendanceRepository
                            .countByStudentIdAndCourseIdAndStatus(
                                    studentId,
                                    courseId,
                                    AttendanceStatus.ABSENT
                            );

            double attendancePercentage = 0.0;

            if (totalSessions > 0) {
                attendancePercentage =
                        ((double) presentCount / totalSessions) * 100.0;
            }

            if (attendancePercentage < threshold) {

                double shortagePercentage =
                        threshold - attendancePercentage;

                response.add(
                        LowAttendanceResponse.builder()
                                .studentId(studentId)
                                .studentName(
                                        student.getUser().getFullName()
                                )
                                .registrationNumber(
                                        student.getRegistrationNumber()
                                )
                                .courseId(course.getId())
                                .courseCode(course.getCourseCode())
                                .courseName(course.getCourseName())
                                .totalSessions(totalSessions)
                                .presentCount(presentCount)
                                .absentCount(absentCount)
                                .attendancePercentage(
                                        Math.round(
                                                attendancePercentage * 100.0
                                        ) / 100.0
                                )
                                .requiredPercentage(threshold)
                                .shortagePercentage(
                                        Math.round(
                                                shortagePercentage * 100.0
                                        ) / 100.0
                                )
                                .build()
                );
            }
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseAttendanceAnalyticsResponse getCourseAttendanceAnalytics(
            Long courseId,
            double threshold) {

        if (threshold < 0 || threshold > 100) {
            throw new RuntimeException(
                    "Attendance threshold must be between 0 and 100"
            );
        }

        Course course =
                courseRepository.findById(courseId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Course not found with ID: " + courseId
                                )
                        );

        long totalStudents =
                enrollmentRepository.countByCourseId(courseId);

        /*
         * Only CLOSED sessions are considered conducted sessions.
         * OPEN sessions are still in progress and therefore are not
         * included in attendance statistics.
         */
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

        double averageAttendancePercentage = 0.0;

        if (totalStudents > 0 && totalSessions > 0) {

            long totalExpectedAttendance =
                    totalStudents * totalSessions;

            averageAttendancePercentage =
                    ((double) totalPresent /
                            totalExpectedAttendance) * 100.0;
        }

        List<LowAttendanceResponse> lowAttendanceStudents =
                getLowAttendanceStudents(courseId, threshold);

        return CourseAttendanceAnalyticsResponse.builder()
                .courseId(course.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .totalStudents(totalStudents)
                .totalSessions(totalSessions)
                .totalPresent(totalPresent)
                .totalAbsent(totalAbsent)
                .averageAttendancePercentage(
                        Math.round(
                                averageAttendancePercentage * 100.0
                        ) / 100.0
                )
                .lowAttendanceStudents(
                        lowAttendanceStudents.size()
                )
                .requiredPercentage(threshold)
                .build();
    }

    // ============================================================
    // GET ATTENDANCE BY DATE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByDate(
            LocalDate date) {

        return attendanceRepository
                .findByAttendanceDate(date)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // ============================================================
    // GET ATTENDANCE BY ID
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getAttendance(Long id) {

        Attendance attendance =
                attendanceRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Attendance record not found with ID: "
                                                + id
                                )
                        );

        return mapToResponse(attendance);
    }


    // ============================================================
    // UPDATE ATTENDANCE
    // ============================================================

    @Override
    public AttendanceResponse updateAttendance(
            Long id,
            AttendanceRequest request) {

        // --------------------------------------------------------
        // 1. Find attendance
        // --------------------------------------------------------

        Attendance attendance =
                attendanceRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Attendance record not found with ID: "
                                                + id
                                )
                        );


        // --------------------------------------------------------
        // 2. Get associated session
        // --------------------------------------------------------

        AttendanceSession session =
                attendance.getSession();

        if (session == null) {

            throw new RuntimeException(
                    "Attendance session not found"
            );
        }


        // --------------------------------------------------------
        // 3. Do not modify closed session
        // --------------------------------------------------------

        if (session.getStatus()
                != AttendanceSessionStatus.OPEN) {

            throw new RuntimeException(
                    "Attendance session is closed. "
                            + "Attendance cannot be modified."
            );
        }


        // --------------------------------------------------------
        // 4. Validate status
        // --------------------------------------------------------

        if (request.getStatus() == null) {

            throw new RuntimeException(
                    "Attendance status is required"
            );
        }


        // --------------------------------------------------------
        // 5. Update status
        // --------------------------------------------------------

        attendance.setStatus(
                request.getStatus()
        );


        // --------------------------------------------------------
        // 6. Save
        // --------------------------------------------------------

        Attendance updatedAttendance =
                attendanceRepository.save(attendance);

        return mapToResponse(updatedAttendance);
    }


    // ============================================================
    // DELETE ATTENDANCE
    // ============================================================

    @Override
    public void deleteAttendance(Long id) {

        // --------------------------------------------------------
        // 1. Find attendance
        // --------------------------------------------------------

        Attendance attendance =
                attendanceRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Attendance record not found with ID: "
                                                + id
                                )
                        );


        // --------------------------------------------------------
        // 2. Get session
        // --------------------------------------------------------

        AttendanceSession session =
                attendance.getSession();

        if (session == null) {

            throw new RuntimeException(
                    "Attendance session not found"
            );
        }


        // --------------------------------------------------------
        // 3. Do not delete from closed session
        // --------------------------------------------------------

        if (session.getStatus()
                != AttendanceSessionStatus.OPEN) {

            throw new RuntimeException(
                    "Attendance session is closed. "
                            + "Attendance cannot be deleted."
            );
        }


        // --------------------------------------------------------
        // 4. Delete
        // --------------------------------------------------------

        attendanceRepository.delete(attendance);
    }


    // ============================================================
    // MAP ENTITY → RESPONSE
    // ============================================================

    private AttendanceResponse mapToResponse(
            Attendance attendance) {

        return AttendanceResponse.builder()

                .id(
                        attendance.getId()
                )

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

                .attendanceDate(
                        attendance.getAttendanceDate()
                )

                .status(
                        attendance.getStatus()
                )

                .build();
    }
}