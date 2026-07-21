package com.example.LVTN.utils;

import com.example.LVTN.entity.User;
import com.example.LVTN.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    @Autowired
    private UserRepository userRepository;

    // Không dùng static nữa để Spring inject an toàn tuyệt đối
    public User getCurrentLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            if (userRepository != null) {
                return userRepository.findByEmail(email).orElse(null);
            }
        }
        return null;
    }

    public String getCurrentRoleName() {
        User user = getCurrentLoggedInUser();
        if (user == null || user.getRole() == null) {
            return "QUẢN TRỊ VIÊN";
        }
        return user.getRole().getRoleName() != null ? user.getRole().getRoleName() : "QUẢN TRỊ VIÊN";
    }
}