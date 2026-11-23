package com.smartattendance.service.impl;

import com.smartattendance.dto.dashboard.AdminDashboardResponse;
import com.smartattendance.dto.dashboard.StudentDashboardResponse;
import com.smartattendance.dto.dashboard.TeacherDashboardResponse;

import com.smartattendance.entity.Student;
import com.smartattendance.entity.Teacher;

import com.smartattendance.enums.AttendanceStatus;

import com.smartattendance.repository.AttendanceRepository;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.EnrollmentRepository;
import com.smartattendance.repository.StudentRepository;
import com.smartattendance.repository.TeacherRepository;

import com.smartattendance.service.DashboardService;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    public AdminDashboardResponse getAdminDashboard() {

        LocalDate today = LocalDate.now();

        return AdminDashboardResponse.builder()
                .totalStudents(studentRepository.count())
                .totalTeachers(teacherRepository.count())
                .totalCourses(courseRepository.count())
                .totalEnrollments(enrollmentRepository.count())
                .todayAttendance(
                        attendanceRepository.countByAttendanceDate(today)
                )
                .presentToday(
                        attendanceRepository.countByAttendanceDateAndStatus(
                                today,
                                AttendanceStatus.PRESENT
                        )
                )
                .absentToday(
                        attendanceRepository.countByAttendanceDateAndStatus(
                                today,
                                AttendanceStatus.ABSENT
                        )
                )
                .build();
    }

    @Override
    public TeacherDashboardResponse getTeacherDashboard(String email) {

        // Identify teacher using authenticated email.
        // Never trust a teacher ID supplied by the frontend.

        Teacher teacher = teacherRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Teacher profile not found"
                ));

        Long teacherId = teacher.getId();

        return TeacherDashboardResponse.builder()
                .teacherName(teacher.getUser().getFullName())
                .totalCourses(
                        courseRepository.countByTeacherId(teacherId)
                )
                .totalStudents(
                        enrollmentRepository.countByCourseTeacherId(teacherId)
                )
                .todayAttendance(
                        attendanceRepository
                                .countByCourseTeacherIdAndAttendanceDate(
                                        teacherId,
                                        LocalDate.now()
                                )
                )
                .build();
    }

    @Override
    public StudentDashboardResponse getStudentDashboard(String email) {

        // Identify student using authenticated email.
        // Never trust a student ID supplied by the frontend.

        Student student = studentRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student profile not found"
                ));

        Long studentId = student.getId();

        long totalClasses =
                attendanceRepository.countByStudentId(studentId);

        long presentClasses =
                attendanceRepository.countByStudentIdAndStatus(
                        studentId,
                        AttendanceStatus.PRESENT
                );

        long absentClasses =
                attendanceRepository.countByStudentIdAndStatus(
                        studentId,
                        AttendanceStatus.ABSENT
                );

        double attendancePercentage = totalClasses == 0
                ? 0.0
                : (presentClasses * 100.0) / totalClasses;

        return StudentDashboardResponse.builder()
                .studentName(student.getUser().getFullName())
                .totalClasses(totalClasses)
                .presentClasses(presentClasses)
                .absentClasses(absentClasses)
                .attendancePercentage(
                        Math.round(attendancePercentage * 100.0) / 100.0
                )
                .build();
    }
}
