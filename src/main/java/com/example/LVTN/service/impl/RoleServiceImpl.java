package com.example.LVTN.service.impl;

import com.example.LVTN.entity.Role;
import com.example.LVTN.repository.RoleRepository;
import com.example.LVTN.service.RoleService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public List<Role> findAll() {

        return roleRepository.findAll();
    }

    @Override
    public Role findById(Long id) {

        return roleRepository.findById(id).orElse(null);
    }
}