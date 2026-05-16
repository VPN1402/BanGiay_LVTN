package com.example.LVTN.service;

import com.example.LVTN.entity.User;

import java.util.List;

public interface UserService {

    void registerUser(String fullName,
                      String email,
                      String phone,
                      String password);

    User checkLogin(String email, String password);

    List<User> findAll();

    User findById(Long id);

    User save(User user);

    void delete(Long id);
}