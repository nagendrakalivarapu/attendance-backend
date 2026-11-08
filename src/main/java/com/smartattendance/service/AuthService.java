
package com.smartattendance.service;

import com.smartattendance.dto.auth.LoginRequest;
import com.smartattendance.dto.auth.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse verifyOtp(
            String email,
            String otp
    );

    void resendOtp(String email);
}
