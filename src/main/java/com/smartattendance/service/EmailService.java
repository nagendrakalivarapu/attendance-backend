package com.smartattendance.service;

public interface EmailService {

    void sendAdminLoginOtp(
            String email,
            String otp
    );
}