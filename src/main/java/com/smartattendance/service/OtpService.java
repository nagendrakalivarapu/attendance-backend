package com.smartattendance.service;

public interface OtpService {

    void generateAndSendOtp(String email);

    boolean verifyOtp(String email, String otp);

    void resendOtp(String email);
}