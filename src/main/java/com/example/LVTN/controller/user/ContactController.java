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


        System.out.println("Nhận liên hệ từ: " + name + " (" + email + ")");
        System.out.println("Chủ đề: " + subject);


        redirectAttributes.addFlashAttribute("successMsg", "Cảm ơn bạn! Tin nhắn đã được gửi đi thành công.");

        return "redirect:/contact";
    }
}