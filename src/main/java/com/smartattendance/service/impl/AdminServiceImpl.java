package com.smartattendance.service.impl;

import com.smartattendance.dto.request.AdminRequest;
import com.smartattendance.dto.response.AdminResponse;
import com.smartattendance.entity.User;
import com.smartattendance.enums.UserRole;
import com.smartattendance.repository.UserRepository;
import com.smartattendance.service.AdminService;
import com.smartattendance.service.PrimaryAdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PrimaryAdminService primaryAdminService;


    // ============================================================
    // GET ALL ADMINS
    // ============================================================

    @Override
    public List<AdminResponse> getAllAdmins() {

        return userRepository.findAll()
                .stream()
                .filter(user ->
                        user.getRole() == UserRole.ADMIN
                )
                .map(this::mapToResponse)
                .toList();
    }


    // ============================================================
    // GET ADMIN BY ID
    // ============================================================

    @Override
    public AdminResponse getAdminById(Long id) {

        User admin = findAdmin(id);

        return mapToResponse(admin);
    }


    // ============================================================
    // CREATE ADMIN
    // ============================================================

    @Override
    public AdminResponse createAdmin(
            AdminRequest request) {

        // Email must be unique
        if (userRepository.existsByEmail(
                request.getEmail())) {

            throw new RuntimeException(
                    "Email already exists."
            );
        }

        User admin = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();

        admin = userRepository.save(admin);

        return mapToResponse(admin);
    }


    // ============================================================
    // UPDATE ADMIN
    // ============================================================

    @Override
    public AdminResponse updateAdmin(
            Long id,
            AdminRequest request) {

        User admin = findAdmin(id);

        // --------------------------------------------------------
        // PRIMARY ADMIN CANNOT BE MODIFIED THROUGH THIS API
        // --------------------------------------------------------

        if (primaryAdminService.isPrimaryAdmin(
                admin.getEmail())) {

            throw new RuntimeException(
                    "Primary admin must be managed through "
                            + "application.properties."
            );
        }


        // --------------------------------------------------------
        // CHECK EMAIL
        // --------------------------------------------------------

        if (request.getEmail() != null &&
                !request.getEmail().isBlank() &&
                !request.getEmail().equalsIgnoreCase(
                        admin.getEmail())) {

            if (userRepository.existsByEmail(
                    request.getEmail())) {

                throw new RuntimeException(
                        "Email already exists."
                );
            }

            admin.setEmail(
                    request.getEmail()
            );
        }


        // --------------------------------------------------------
        // UPDATE NAME
        // --------------------------------------------------------

        if (request.getFullName() != null &&
                !request.getFullName().isBlank()) {

            admin.setFullName(
                    request.getFullName()
            );
        }


        // --------------------------------------------------------
        // UPDATE PASSWORD
        // --------------------------------------------------------

        if (request.getPassword() != null &&
                !request.getPassword().isBlank()) {

            admin.setPassword(
                    passwordEncoder.encode(
                            request.getPassword()
                    )
            );
        }

        admin = userRepository.save(admin);

        return mapToResponse(admin);
    }


    // ============================================================
    // DELETE ADMIN
    // ============================================================

    @Override
    @Transactional
    public void deleteAdmin(Long id) {

        User admin = findAdmin(id);

        // --------------------------------------------------------
        // PRIMARY ADMIN CANNOT BE DELETED
        // --------------------------------------------------------

        if (primaryAdminService.isPrimaryAdmin(
                admin.getEmail())) {

            throw new RuntimeException(
                    "Primary admin cannot be deleted."
            );
        }

        userRepository.delete(admin);
    }


    // ============================================================
    // FIND ADMIN
    // ============================================================

    private User findAdmin(Long id) {

        User admin = userRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Admin not found."
                        )
                );

        if (admin.getRole() != UserRole.ADMIN) {

            throw new RuntimeException(
                    "The selected user is not an admin."
            );
        }

        return admin;
    }


    // ============================================================
    // MAP ENTITY TO RESPONSE
    // ============================================================

    private AdminResponse mapToResponse(
            User admin) {

        return AdminResponse.builder()
                .id(admin.getId())
                .fullName(admin.getFullName())
                .email(admin.getEmail())
                .role(admin.getRole().name())
                .enabled(admin.getEnabled())
                .build();
    }
}
