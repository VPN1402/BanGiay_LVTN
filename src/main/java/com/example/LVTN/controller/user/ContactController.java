package com.example.LVTN.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {

    @GetMapping("/contact")
    public String showContactPage(Model model, HttpServletRequest request) {
        // Gửi currentUri để navbar nhận biết trang active
        model.addAttribute("currentUri", request.getRequestURI());
        return "user/contact/contact.html";
    }

    @PostMapping("/contact/send")
    public String handleContactForm(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String subject,
            @RequestParam String message,
            RedirectAttributes redirectAttributes) {

        // Bước này bạn có thể xử lý: Lưu vào DB hoặc gửi Email thực tế
        System.out.println("Nhận liên hệ từ: " + name + " (" + email + ")");
        System.out.println("Chủ đề: " + subject);

        // Thông báo cho người dùng sau khi gửi thành công
        redirectAttributes.addFlashAttribute("successMsg", "Cảm ơn bạn! Tin nhắn đã được gửi đi thành công.");

        return "redirect:/contact";
    }
}