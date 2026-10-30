package com.smartattendance.service.impl;

import com.smartattendance.attendance.entity.AttendanceSession;
import com.smartattendance.attendance.repository.AttendanceSessionRepository;
import com.smartattendance.dto.request.AttendanceEmailRequest;
import com.smartattendance.dto.response.AttendanceEmailPreviewResponse;
import com.smartattendance.dto.response.AttendanceEmailSendResponse;
import com.smartattendance.dto.response.AttendanceEmailStudentResponse;
import com.smartattendance.entity.Course;
import com.smartattendance.entity.Enrollment;
import com.smartattendance.entity.Student;
import com.smartattendance.enums.AttendanceSessionStatus;
import com.smartattendance.enums.AttendanceStatus;
import com.smartattendance.enums.EnrollmentStatus;
import com.smartattendance.repository.AttendanceRepository;
import com.smartattendance.repository.CourseRepository;
import com.smartattendance.repository.EnrollmentRepository;
import com.smartattendance.service.AttendanceEmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceEmailServiceImpl
        implements AttendanceEmailService {

    private final CourseRepository courseRepository;

    private final EnrollmentRepository enrollmentRepository;

    private final AttendanceRepository attendanceRepository;

    private final AttendanceSessionRepository attendanceSessionRepository;

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;


    // ============================================================
    // PREVIEW
    // ============================================================

    @Override
    public AttendanceEmailPreviewResponse previewAttendanceEmails(
            AttendanceEmailRequest request) {

        validateRequest(request);

        Course course = getCourse(request.getCourseId());

        validateCourseSelection(course, request);

        List<AttendanceEmailStudentResponse> students =
                buildStudentReports(request, course);

        long lowAttendanceStudents =
                students.stream()
                        .filter(student ->
                                "LOW_ATTENDANCE"
                                        .equals(student.getStatus()))
                        .count();

        long goodAttendanceStudents =
                students.size() - lowAttendanceStudents;

        return AttendanceEmailPreviewResponse.builder()
                .courseId(course.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .department(request.getDepartment())
                .semester(request.getSemester())
                .section(request.getSection())
                .requiredPercentage(request.getThreshold())
                .totalStudents(students.size())
                .goodAttendanceStudents(goodAttendanceStudents)
                .lowAttendanceStudents(lowAttendanceStudents)
                .students(students)
                .build();
    }


    // ============================================================
    // SEND EMAILS
    // ============================================================

    @Override
    @Transactional
    public AttendanceEmailSendResponse sendAttendanceEmails(
            AttendanceEmailRequest request) {

        validateRequest(request);

        if (mailUsername == null || mailUsername.isBlank()) {

            throw new RuntimeException(
                    "MAIL_USERNAME is not configured. " +
                    "Configure the SMTP email account before sending."
            );
        }

        AttendanceEmailPreviewResponse preview =
                previewAttendanceEmails(request);

        List<String> sentEmails = new ArrayList<>();

        List<String> failedEmails = new ArrayList<>();


        for (AttendanceEmailStudentResponse student
                : preview.getStudents()) {

            if (student.getEmail() == null ||
                    student.getEmail().isBlank()) {

                failedEmails.add(
                        student.getRegistrationNumber()
                                + " (missing email)"
                );

                continue;
            }

            try {

                sendStudentEmail(
                        student,
                        preview.getCourseCode(),
                        preview.getCourseName()
                );

                sentEmails.add(student.getEmail());

            } catch (MailException |
                     MessagingException exception) {

                failedEmails.add(student.getEmail());
            }
        }


        String message;

        if (failedEmails.isEmpty()) {

            message =
                    "Attendance reports sent successfully " +
                    "to all students.";

        } else if (sentEmails.isEmpty()) {

            message =
                    "No attendance reports could be sent.";

        } else {

            message =
                    "Attendance reports processed. " +
                    "Some emails failed.";
        }


        return AttendanceEmailSendResponse.builder()
                .totalStudents(preview.getStudents().size())
                .sentCount(sentEmails.size())
                .failedCount(failedEmails.size())
                .sentEmails(sentEmails)
                .failedEmails(failedEmails)
                .message(message)
                .build();
    }


    // ============================================================
    // BUILD STUDENT REPORTS
    // ============================================================

    private List<AttendanceEmailStudentResponse>
    buildStudentReports(
            AttendanceEmailRequest request,
            Course course) {

        List<Enrollment> enrollments =
                enrollmentRepository.findByCourseId(
                        course.getId()
                );

        long totalSessions =
                attendanceSessionRepository
                        .countByCourseIdAndStatus(
                                course.getId(),
                                AttendanceSessionStatus.CLOSED
                        );

        List<AttendanceEmailStudentResponse> response =
                new ArrayList<>();


        for (Enrollment enrollment : enrollments) {

            if (enrollment.getStatus() != null &&
                    enrollment.getStatus()
                            != EnrollmentStatus.ACTIVE) {

                continue;
            }

            Student student = enrollment.getStudent();

            if (student == null) {
                continue;
            }


            // Department filter
            if (student.getDepartment() == null ||
                    !student.getDepartment()
                            .equalsIgnoreCase(
                                    request.getDepartment()
                            )) {

                continue;
            }


            // Semester filter
            if (student.getSemester() == null ||
                    !student.getSemester()
                            .equals(request.getSemester())) {

                continue;
            }


            // Section filter
            if (student.getSection() == null ||
                    !student.getSection()
                            .equalsIgnoreCase(
                                    request.getSection()
                            )) {

                continue;
            }


            long presentCount =
                    attendanceRepository
                            .countByStudentIdAndCourseIdAndStatus(
                                    student.getId(),
                                    course.getId(),
                                    AttendanceStatus.PRESENT
                            );


            long absentCount =
                    attendanceRepository
                            .countByStudentIdAndCourseIdAndStatus(
                                    student.getId(),
                                    course.getId(),
                                    AttendanceStatus.ABSENT
                            );


            double attendancePercentage = 0.0;


            if (totalSessions > 0) {

                attendancePercentage =
                        ((double) presentCount
                                / totalSessions) * 100.0;
            }


            attendancePercentage =
                    Math.round(
                            attendancePercentage * 100.0
                    ) / 100.0;


            String status =
                    attendancePercentage
                            < request.getThreshold()
                            ? "LOW_ATTENDANCE"
                            : "GOOD";


            String email = null;

            if (student.getUser() != null) {

                email = student.getUser().getEmail();
            }


            String studentName =
                    student.getRegistrationNumber();

            if (student.getUser() != null &&
                    student.getUser().getFullName() != null) {

                studentName =
                        student.getUser().getFullName();
            }


            response.add(
                    AttendanceEmailStudentResponse.builder()

                            .studentId(student.getId())

                            .studentName(studentName)

                            .registrationNumber(
                                    student.getRegistrationNumber()
                            )

                            .email(email)

                            .department(
                                    student.getDepartment()
                            )

                            .semester(
                                    student.getSemester()
                            )

                            .section(
                                    student.getSection()
                            )

                            .totalSessions(
                                    totalSessions
                            )

                            .presentCount(
                                    presentCount
                            )

                            .absentCount(
                                    absentCount
                            )

                            .attendancePercentage(
                                    attendancePercentage
                            )

                            .requiredPercentage(
                                    request.getThreshold()
                            )

                            .status(status)

                            .build()
            );
        }


        return response;
    }


    // ============================================================
    // SEND INDIVIDUAL EMAIL
    // ============================================================

    private void sendStudentEmail(
            AttendanceEmailStudentResponse student,
            String courseCode,
            String courseName)
            throws MessagingException {

        MimeMessage message =
                mailSender.createMimeMessage();


        MimeMessageHelper helper =
                new MimeMessageHelper(
                        message,
                        false,
                        "UTF-8"
                );


        helper.setFrom(mailUsername);

        helper.setTo(student.getEmail());


        helper.setSubject(
                "Attendance Report - "
                        + courseCode
                        + " - "
                        + courseName
        );


        helper.setText(
                buildEmailHtml(
                        student,
                        courseCode,
                        courseName
                ),
                true
        );


        mailSender.send(message);
    }


    // ============================================================
    // EMAIL HTML
    // ============================================================

    private String buildEmailHtml(
            AttendanceEmailStudentResponse student,
            String courseCode,
            String courseName) {

        boolean lowAttendance =
                "LOW_ATTENDANCE"
                        .equals(student.getStatus());


        String statusText =
                lowAttendance
                        ? "LOW ATTENDANCE"
                        : "GOOD ATTENDANCE";


        String statusColor =
                lowAttendance
                        ? "#b42318"
                        : "#067647";


        return """
                <!DOCTYPE html>
                <html>
                <body style="
                    margin:0;
                    padding:0;
                    background:#f5f7fb;
                    font-family:Arial,sans-serif;
                    color:#1f2937;
                ">

                <div style="
                    max-width:680px;
                    margin:30px auto;
                    background:#ffffff;
                    border:1px solid #e5e7eb;
                    border-radius:12px;
                    overflow:hidden;
                ">

                    <div style="
                        padding:24px;
                        background:#111827;
                        color:#ffffff;
                    ">

                        <h2 style="
                            margin:0 0 6px;
                        ">
                            Smart Attendance System
                        </h2>

                        <p style="
                            margin:0;
                            color:#d1d5db;
                        ">
                            Attendance Report
                        </p>

                    </div>


                    <div style="padding:28px;">

                        <p>
                            Dear
                            <strong>%s</strong>,
                        </p>

                        <p>
                            Please find your attendance report
                            for
                            <strong>%s (%s)</strong>.
                        </p>


                        <table style="
                            width:100%%;
                            border-collapse:collapse;
                            margin:24px 0;
                        ">

                            <tr>
                                <td style="
                                    padding:10px;
                                    border-bottom:1px solid #e5e7eb;
                                ">
                                    Registration Number
                                </td>

                                <td style="
                                    padding:10px;
                                    border-bottom:1px solid #e5e7eb;
                                ">
                                    <strong>%s</strong>
                                </td>
                            </tr>


                            <tr>
                                <td style="
                                    padding:10px;
                                    border-bottom:1px solid #e5e7eb;
                                ">
                                    Section
                                </td>

                                <td style="
                                    padding:10px;
                                    border-bottom:1px solid #e5e7eb;
                                ">
                                    <strong>%s</strong>
                                </td>
                            </tr>


                            <tr>
                                <td style="
                                    padding:10px;
                                    border-bottom:1px solid #e5e7eb;
                                ">
                                    Total Classes
                                </td>

                                <td style="
                                    padding:10px;
                                    border-bottom:1px solid #e5e7eb;
                                ">
                                    <strong>%d</strong>
                                </td>
                            </tr>


                            <tr>
                                <td style="
                                    padding:10px;
                                    border-bottom:1px solid #e5e7eb;
                                ">
                                    Present
                                </td>

                                <td style="
                                    padding:10px;
                                    border-bottom:1px solid #e5e7eb;
                                ">
                                    <strong>%d</strong>
                                </td>
                            </tr>


                            <tr>
                                <td style="
                                    padding:10px;
                                    border-bottom:1px solid #e5e7eb;
                                ">
                                    Absent
                                </td>

                                <td style="
                                    padding:10px;
                                    border-bottom:1px solid #e5e7eb;
                                ">
                                    <strong>%d</strong>
                                </td>
                            </tr>


                            <tr>
                                <td style="padding:10px;">
                                    Attendance Percentage
                                </td>

                                <td style="padding:10px;">
                                    <strong>%.2f%%</strong>
                                </td>
                            </tr>

                        </table>


                        <div style="
                            padding:14px 16px;
                            background:#f9fafb;
                            border-left:4px solid %s;
                        ">

                            <strong style="color:%s;">
                                Status: %s
                            </strong>

                            <br/>

                            <span>
                                Required attendance:
                                %.2f%%
                            </span>

                        </div>


                        <p style="margin-top:24px;">
                            Please maintain the required attendance
                            percentage for the course.
                        </p>


                        <p>
                            Regards,<br/>
                            <strong>
                                Smart Attendance System
                            </strong>
                        </p>

                    </div>

                </div>

                </body>
                </html>
                """
                .formatted(
                        escapeHtml(
                                student.getStudentName()
                        ),

                        escapeHtml(courseName),

                        escapeHtml(courseCode),

                        escapeHtml(
                                student.getRegistrationNumber()
                        ),

                        escapeHtml(
                                student.getSection()
                        ),

                        student.getTotalSessions(),

                        student.getPresentCount(),

                        student.getAbsentCount(),

                        student.getAttendancePercentage(),

                        statusColor,

                        statusColor,

                        statusText,

                        student.getRequiredPercentage()
                );
    }


    // ============================================================
    // COURSE VALIDATION
    // ============================================================

    private void validateCourseSelection(
            Course course,
            AttendanceEmailRequest request) {

        if (course.getDepartment() == null ||
                !course.getDepartment()
                        .equalsIgnoreCase(
                                request.getDepartment()
                        )) {

            throw new RuntimeException(
                    "Selected course does not belong to department "
                            + request.getDepartment()
            );
        }


        if (course.getSemester() == null ||
                !course.getSemester()
                        .equals(request.getSemester())) {

            throw new RuntimeException(
                    "Selected course does not belong to semester "
                            + request.getSemester()
            );
        }
    }


    // ============================================================
    // GET COURSE
    // ============================================================

    private Course getCourse(Long courseId) {

        return courseRepository
                .findById(courseId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Course not found with ID: "
                                        + courseId
                        )
                );
    }


    // ============================================================
    // REQUEST VALIDATION
    // ============================================================

    private void validateRequest(
            AttendanceEmailRequest request) {

        if (request == null) {

            throw new RuntimeException(
                    "Attendance email request is required."
            );
        }


        if (request.getCourseId() == null) {

            throw new RuntimeException(
                    "Course ID is required."
            );
        }


        if (request.getDepartment() == null ||
                request.getDepartment().isBlank()) {

            throw new RuntimeException(
                    "Department is required."
            );
        }


        if (request.getSemester() == null) {

            throw new RuntimeException(
                    "Semester is required."
            );
        }


        if (request.getSection() == null ||
                request.getSection().isBlank()) {

            throw new RuntimeException(
                    "Section is required."
            );
        }


        if (request.getThreshold() < 0 ||
                request.getThreshold() > 100) {

            throw new RuntimeException(
                    "Attendance threshold must be between 0 and 100."
            );
        }
    }


    // ============================================================
    // HTML ESCAPE
    // ============================================================

    private String escapeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}