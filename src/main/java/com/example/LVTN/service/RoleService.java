package com.example.LVTN.service;

import com.example.LVTN.entity.Role;

import java.util.List;

public interface RoleService {

    List<Role> findAll();

    Role findById(Long id);
}