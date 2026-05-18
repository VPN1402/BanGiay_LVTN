package com.example.LVTN.controller.user;

import com.example.LVTN.entity.Contact;
import com.example.LVTN.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping; // Thêm import này
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {

    @Autowired
    private ContactRepository contactRepository;


    @GetMapping("/contact")
    public String showContactPage() {
        return "user/contact/contact";
    }

    @PostMapping("/contact/send")
    public String handleContactForm(@ModelAttribute Contact contact, RedirectAttributes redirectAttributes) {
        contactRepository.save(contact);
        redirectAttributes.addFlashAttribute("successMsg", "Tin nhắn của bạn đã được gửi đi thành công! Chúng tôi sẽ phản hồi sớm nhất.");
        return "redirect:/contact";
    }
}