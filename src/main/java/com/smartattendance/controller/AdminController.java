package com.smartattendance.controller;

import com.smartattendance.dto.request.AdminRequest;
import com.smartattendance.dto.response.AdminResponse;
import com.smartattendance.service.AdminService;
import com.smartattendance.service.PrimaryAdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PrimaryAdminService primaryAdminService;

    // ============================================================
    // GET ALL ADMINS
    // PRIMARY ADMIN ONLY
    // ============================================================

    @GetMapping
    public List<AdminResponse> getAllAdmins(
            Authentication authentication) {

        requirePrimaryAdmin(authentication);

        return adminService.getAllAdmins();
    }

    // ============================================================
    // GET ADMIN
    // PRIMARY ADMIN ONLY
    // ============================================================

    @GetMapping("/{id}")
    public AdminResponse getAdminById(
            @PathVariable Long id,
            Authentication authentication) {

        requirePrimaryAdmin(authentication);

        return adminService.getAdminById(id);
    }

    // ============================================================
    // CREATE ADMIN
    // PRIMARY ADMIN ONLY
    // ============================================================

    @PostMapping
    public AdminResponse createAdmin(
            @Valid @RequestBody AdminRequest request,
            Authentication authentication) {

        requirePrimaryAdmin(authentication);

        return adminService.createAdmin(request);
    }

    // ============================================================
    // UPDATE ADMIN
    // PRIMARY ADMIN ONLY
    // ============================================================

    @PutMapping("/{id}")
    public AdminResponse updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AdminRequest request,
            Authentication authentication) {

        requirePrimaryAdmin(authentication);

        return adminService.updateAdmin(
                id,
                request
        );
    }

    // ============================================================
    // DELETE ADMIN
    // PRIMARY ADMIN ONLY
    // ============================================================

    @DeleteMapping("/{id}")
    public String deleteAdmin(
            @PathVariable Long id,
            Authentication authentication) {

        requirePrimaryAdmin(authentication);

        adminService.deleteAdmin(id);

        return "Admin deleted successfully.";
    }

    // ============================================================
    // PRIMARY ADMIN AUTHORIZATION
    // ============================================================

    private void requirePrimaryAdmin(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required."
            );
        }

        String email =
                authentication.getName();

        if (!primaryAdminService.isPrimaryAdmin(email)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the primary admin can manage admins."
            );
        }
    }
}
