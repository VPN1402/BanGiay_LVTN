package com.example.LVTN.service;

import com.example.LVTN.entity.Role;
import com.example.LVTN.entity.User;
import com.example.LVTN.repository.RoleRepository;
import com.example.LVTN.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    public void registerUser(String fullName, String email, String phone, String password) {

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email này đã được sử dụng!");
        }


        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        String encodedPassword = passwordEncoder.encode(password);
        newUser.setPassword(encodedPassword);
        newUser.setStatus(1);
        Role userRole = roleRepository.findByRoleName("ROLE_USER");


        newUser.setRole(userRole);// mặc định khi dk là user

        userRepository.save(newUser);
    }
    public User checkLogin(String email, String password) {
        // Tìm trong database theo cột email
        return userRepository.findByEmail(email)
                .filter(user -> user.getPassword().equals(password))
                .orElse(null);
    }
}