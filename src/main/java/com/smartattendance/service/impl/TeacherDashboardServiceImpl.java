package com.smartattendance.service.impl;

import com.smartattendance.attendance.repository.AttendanceSessionRepository;
import com.smartattendance.dto.response.TeacherCourseSummaryResponse;
import com.smartattendance.dto.response.TeacherDashboardResponse;
import com.smartattendance.entity.Course;
import com.smartattendance.entity.Enrollment;
import com.smartattendance.entity.Teacher;
import com.smartattendance.enums.AttendanceSessionStatus;
import com.smartattendance.enums.AttendanceStatus;
import com.smartattendance.repository.AttendanceRepository;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.EnrollmentRepository;
import com.smartattendance.repository.TeacherRepository;
import com.smartattendance.service.TeacherDashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherDashboardServiceImpl
        implements TeacherDashboardService {

    private final TeacherRepository teacherRepository;

    private final CourseRepository courseRepository;

    private final EnrollmentRepository enrollmentRepository;

    private final AttendanceRepository attendanceRepository;

    private final AttendanceSessionRepository attendanceSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public TeacherDashboardResponse getTeacherDashboard() {

        /*
         * Get logged-in user's email from JWT/Spring Security.
         */
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

        /*
         * Find teacher using the email stored in users table.
         */
        Teacher teacher =
                teacherRepository
                        .findByUser_Email(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Teacher not found for email: "
                                                + email
                                )
                        );

        /*
         * Find all courses belonging to this teacher.
         */
        List<Course> courses =
                courseRepository.findByTeacherId(
                        teacher.getId()
                );

        List<TeacherCourseSummaryResponse> courseResponses =
                new ArrayList<>();

        long totalStudents = 0;
        long totalSessions = 0;
        long totalPresent = 0;
        long totalAbsent = 0;
        long lowAttendanceStudents = 0;

        final double REQUIRED_PERCENTAGE = 75.0;

        for (Course course : courses) {

            Long courseId = course.getId();

            /*
             * Number of active students enrolled in this course.
             */
            long courseStudents =
                    enrollmentRepository
                            .findByCourseId(courseId)
                            .stream()
                            .filter(enrollment ->
                                    enrollment.getStatus() != null &&
                                    enrollment.getStatus()
                                            .name()
                                            .equals("ACTIVE")
                            )
                            .count();

            /*
             * Only CLOSED sessions count as conducted sessions.
             */
            long courseSessions =
                    attendanceSessionRepository
                            .countByCourseIdAndStatus(
                                    courseId,
                                    AttendanceSessionStatus.CLOSED
                            );

            long coursePresent =
                    attendanceRepository
                            .countByCourseIdAndStatus(
                                    courseId,
                                    AttendanceStatus.PRESENT
                            );

            long courseAbsent =
                    attendanceRepository
                            .countByCourseIdAndStatus(
                                    courseId,
                                    AttendanceStatus.ABSENT
                            );

            /*
             * Calculate course attendance percentage.
             */
            long expectedAttendance =
                    courseStudents * courseSessions;

            double coursePercentage = 0.0;

            if (expectedAttendance > 0) {

                coursePercentage =
                        ((double) coursePresent /
                                expectedAttendance) * 100.0;
            }

            /*
             * Find students below 75%.
             */
            List<Enrollment> enrollments =
                    enrollmentRepository
                            .findByCourseId(courseId);

            long courseLowAttendance = 0;

            for (Enrollment enrollment : enrollments) {

                if (enrollment.getStatus() == null ||
                        !enrollment.getStatus()
                                .name()
                                .equals("ACTIVE")) {

                    continue;
                }

                if (enrollment.getStudent() == null) {
                    continue;
                }

                Long studentId =
                        enrollment.getStudent().getId();

                long present =
                        attendanceRepository
                                .countByStudentIdAndCourseIdAndStatus(
                                        studentId,
                                        courseId,
                                        AttendanceStatus.PRESENT
                                );

                double studentPercentage = 0.0;

                if (courseSessions > 0) {

                    studentPercentage =
                            ((double) present /
                                    courseSessions) * 100.0;
                }

                if (studentPercentage < REQUIRED_PERCENTAGE) {
                    courseLowAttendance++;
                }
            }

            /*
             * Add course summary.
             */
            courseResponses.add(
                    TeacherCourseSummaryResponse.builder()
                            .courseId(courseId)
                            .courseCode(course.getCourseCode())
                            .courseName(course.getCourseName())
                            .totalStudents(courseStudents)
                            .totalSessions(courseSessions)
                            .totalPresent(coursePresent)
                            .totalAbsent(courseAbsent)
                            .attendancePercentage(
                                    Math.round(
                                            coursePercentage * 100.0
                                    ) / 100.0
                            )
                            .lowAttendanceStudents(
                                    courseLowAttendance
                            )
                            .build()
            );

            /*
             * Add to teacher totals.
             */
            totalStudents += courseStudents;
            totalSessions += courseSessions;
            totalPresent += coursePresent;
            totalAbsent += courseAbsent;
            lowAttendanceStudents += courseLowAttendance;
        }

        /*
         * Calculate teacher's overall attendance.
         *
         * We calculate this from all expected attendance
         * records across the teacher's courses.
         */
        long totalExpectedAttendance = 0;

        for (TeacherCourseSummaryResponse course :
                courseResponses) {

            totalExpectedAttendance +=
                    course.getTotalStudents() *
                            course.getTotalSessions();
        }

        double averageAttendancePercentage = 0.0;

        if (totalExpectedAttendance > 0) {

            averageAttendancePercentage =
                    ((double) totalPresent /
                            totalExpectedAttendance) * 100.0;
        }

        /*
         * Build final dashboard response.
         */
        return TeacherDashboardResponse.builder()
                .teacherId(teacher.getId())
                .teacherName(
                        teacher.getUser().getFullName()
                )
                .totalCourses(courses.size())
                .totalStudents(totalStudents)
                .totalSessions(totalSessions)
                .averageAttendancePercentage(
                        Math.round(
                                averageAttendancePercentage * 100.0
                        ) / 100.0
                )
                .lowAttendanceStudents(
                        lowAttendanceStudents
                )
                .courses(courseResponses)
                .build();
    }
}