package com.example.LVTN.controller.auth;

import com.example.LVTN.entity.User;
import com.example.LVTN.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/auth/login")
    public String loginPage(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI());
        return "auth/login";
    }
    @PostMapping("/auth/login")
    public String handleLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session, // Nhận đối tượng session từ Spring để cất dữ liệu
            RedirectAttributes redirectAttributes) {

        try {

            User user = userService.checkLogin(email, password);

            if (user != null) {
                // lưu vào session với key loggedInUser
                session.setAttribute("loggedInUser", user);


                return "redirect:/";
            } else {
                redirectAttributes.addFlashAttribute("errorMsg", "Email hoặc mật khẩu không chính xác!");
                return "redirect:/auth/login";
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Đăng nhập thất bại: " + e.getMessage());
            return "redirect:/auth/login";
        }
    }


    @GetMapping("/auth/register")
    public String registerPage(Model model, HttpServletRequest request) {
        model.addAttribute("currentUri", request.getRequestURI());
        return "auth/register";
    }


    @PostMapping("/auth/register")
    public String handleRegister(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        try {

            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute(
                        "errorMsg",
                        "Mật khẩu xác nhận không khớp!"
                );
                return "redirect:/auth/register";
            }

            userService.registerUser(
                    fullName,
                    email,
                    phone,
                    password
            );

            redirectAttributes.addFlashAttribute(
                    "successMsg",
                    "Đăng ký thành công! Mời bạn đăng nhập."
            );

            return "redirect:/auth/login";

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMsg",
                    e.getMessage()
            );

            return "redirect:/auth/register";
        }
    }
}