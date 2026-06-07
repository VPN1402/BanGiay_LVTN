package com.example.LVTN.controller.user;

import com.example.LVTN.entity.User;
import com.example.LVTN.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication; // Import thêm thư viện này
import org.springframework.security.core.context.SecurityContextHolder; // Import thêm thư viện này
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.servlet.http.HttpSession;

@Controller
public class ProfileController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public String showProfile(Model model, HttpSession session){

        // CÁCH MỚI: Lấy thông tin xác thực trực tiếp từ gốc của Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra nếu chưa đăng nhập hoặc là tài khoản ẩn danh (anonymousUser) của Spring Security
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/auth/login";
        }

        // Spring Security lưu username chính là cái trường ta cấu hình đăng nhập (chính là Email của bạn)
        String currentEmail = authentication.getName();

        // Tìm User dưới Database bằng Email vừa lấy được
        User user = userService.findByEmail(currentEmail);

        if (user == null) {
            return "redirect:/auth/login";
        }

        // Đồng bộ ngược lại vào session cũ để đảm bảo các tính năng khác không bị ảnh hưởng
        session.setAttribute("loggedInUser", user);

        // Đẩy dữ liệu ra ngoài giao diện Thymeleaf
        model.addAttribute("user", user);
        model.addAttribute("currentUri", "/profile");

        return "user/profile/list";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("user") User userForm, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/auth/login";
        }

        String currentEmail = authentication.getName();
        User userInDb = userService.findByEmail(currentEmail);

        if (userInDb != null) {
            userInDb.setFullName(userForm.getFullName());
            userInDb.setPhone(userForm.getPhone());
            userService.save(userInDb);

            // Làm mới session
            session.setAttribute("loggedInUser", userInDb);
        }

        return "redirect:/profile?success=true";
    }
}