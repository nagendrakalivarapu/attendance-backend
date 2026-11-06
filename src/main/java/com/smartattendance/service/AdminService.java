package com.smartattendance.service;

import com.smartattendance.dto.request.AdminRequest;
import com.smartattendance.dto.response.AdminResponse;

import java.util.List;

public interface AdminService {

    List<AdminResponse> getAllAdmins();

    AdminResponse getAdminById(Long id);

    AdminResponse createAdmin(AdminRequest request);

    AdminResponse updateAdmin(Long id, AdminRequest request);

    void deleteAdmin(Long id);
}