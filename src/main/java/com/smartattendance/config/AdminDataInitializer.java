package com.smartattendance.config;

import com.smartattendance.entity.User;
import com.smartattendance.enums.UserRole;
import com.smartattendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
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

        User admin = userRepository
                .findByEmail(adminEmail)
                .orElse(null);

        // --------------------------------------------------------
        // CREATE PRIMARY ADMIN IF IT DOES NOT EXIST
        // --------------------------------------------------------

        if (admin == null) {

            admin = User.builder()
                    .fullName(adminName)
                    .email(adminEmail)
                    .password(
                            passwordEncoder.encode(adminPassword)
                    )
                    .role(UserRole.ADMIN)
                    .enabled(true)
                    .build();

            userRepository.save(admin);

            System.out.println(
                    "=================================================="
            );
            System.out.println(
                    "PRIMARY ADMIN CREATED"
            );
            System.out.println(
                    "Email: " + adminEmail
            );
            System.out.println(
                    "=================================================="
            );

            return;
        }

        // --------------------------------------------------------
        // ENSURE PRIMARY ADMIN REMAINS AN ADMIN
        // --------------------------------------------------------

        boolean changed = false;

        if (!adminName.equals(admin.getFullName())) {

            admin.setFullName(adminName);
            changed = true;
        }

        if (admin.getRole() != UserRole.ADMIN) {

            admin.setRole(UserRole.ADMIN);
            changed = true;
        }

        if (!Boolean.TRUE.equals(admin.getEnabled())) {

            admin.setEnabled(true);
            changed = true;
        }

        // Update password when application.properties changes
        if (!passwordEncoder.matches(
                adminPassword,
                admin.getPassword())) {

            admin.setPassword(
                    passwordEncoder.encode(adminPassword)
            );

            changed = true;
        }

        if (changed) {

            userRepository.save(admin);

            System.out.println(
                    "PRIMARY ADMIN CONFIGURATION UPDATED"
            );

        } else {

            System.out.println(
                    "PRIMARY ADMIN ALREADY CONFIGURED"
            );
        }
    }
}
