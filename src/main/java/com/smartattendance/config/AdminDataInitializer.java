
package com.smartattendance.config;

import com.smartattendance.entity.User;
import com.smartattendance.enums.UserRole;
import com.smartattendance.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.name}")
    private String adminName;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {

        validateConfiguration();

        // Never modify an existing account during application startup.
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Configured initial administrator already exists.");
            return;
        }

        User admin = User.builder()
                .fullName(adminName.trim())
                .email(adminEmail.trim())
                .password(passwordEncoder.encode(adminPassword))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);

        log.info("Initial administrator account created successfully.");
    }

    private void validateConfiguration() {

        if (adminName == null || adminName.isBlank()) {
            throw new IllegalStateException(
                    "Admin name configuration is missing."
            );
        }

        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException(
                    "Admin email configuration is missing."
            );
        }

        if (adminPassword == null || adminPassword.length() < 12) {
            throw new IllegalStateException(
                    "Initial admin password must contain at least 12 characters."
            );
        }
    }
}
