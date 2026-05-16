package com.example.LVTN.service.impl;

import com.example.LVTN.entity.Role;
import com.example.LVTN.entity.User;
import com.example.LVTN.repository.RoleRepository;
import com.example.LVTN.repository.UserRepository;
import com.example.LVTN.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;



    @Override
    public void registerUser(String fullName,
                             String email,
                             String phone,
                             String password) {

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email này đã được sử dụng!");
        }

        User newUser = new User();

        newUser.setFullName(fullName);

        newUser.setEmail(email);

        newUser.setPhone(phone);

        // mã hóa pas
        String encodedPassword = passwordEncoder.encode(password);

        newUser.setPassword(encodedPassword);

        newUser.setStatus(1);

        // role mặc định
        Role userRole = roleRepository.findByRoleName("ROLE_USER");

        newUser.setRole(userRole);

        userRepository.save(newUser);
    }



    @Override
    public User checkLogin(String email,
                           String password) {

        return userRepository.findByEmail(email)
                .filter(user ->
                        passwordEncoder.matches(password,
                                user.getPassword()))
                .orElse(null);
    }



    @Override
    public List<User> findAll() {

        return userRepository.findAll();
    }

    @Override
    public User findById(Long id) {

        return userRepository.findById(id).orElse(null);
    }

    @Override
    public User save(User user) {

        return userRepository.save(user);
    }

    @Override
    public void delete(Long id) {

        userRepository.deleteById(id);
    }
}