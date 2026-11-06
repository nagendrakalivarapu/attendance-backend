package com.smartattendance.service.impl;

import com.smartattendance.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendAdminLoginOtp(
            String email,
            String otp
    ) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(email);

        message.setSubject(
                "Smart Attendance - Admin Login OTP"
        );

        message.setText(
                "Hello,\n\n"
                        + "Your Smart Attendance administrator login OTP is:\n\n"
                        + otp
                        + "\n\n"
                        + "This OTP is valid for 5 minutes.\n\n"
                        + "For security reasons, do not share this OTP with anyone.\n\n"
                        + "If you did not attempt to log in, please ignore this email.\n\n"
                        + "Smart Attendance System"
        );

        mailSender.send(message);
    }
}