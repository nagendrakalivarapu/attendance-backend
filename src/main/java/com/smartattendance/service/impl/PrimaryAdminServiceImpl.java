package com.smartattendance.service.impl;

import com.smartattendance.service.PrimaryAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrimaryAdminServiceImpl
        implements PrimaryAdminService {

    @Value("${admin.email}")
    private String primaryAdminEmail;

    @Override
    public boolean isPrimaryAdmin(String email) {

        if (email == null || email.isBlank()) {
            return false;
        }

        return primaryAdminEmail.equalsIgnoreCase(email);
    }
}
