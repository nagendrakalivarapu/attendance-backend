package com.smartattendance.service;

import com.smartattendance.entity.User;

import java.util.List;

public interface UserService {

    User save(User user);

    List<User> findAll();

}