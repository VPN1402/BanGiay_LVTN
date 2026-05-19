package com.example.LVTN.controller.admin;

import com.example.LVTN.entity.Contact;
import com.example.LVTN.repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
@Controller
@RequestMapping("/admin")
public class AdminContactController {
    @Autowired
    private ContactRepository contactRepository;

    // --- CONTACTS ---
    @GetMapping("/contacts")
    public String manageContacts(Model model) {
        List<Contact> contacts = contactRepository.findAll();
        model.addAttribute("contacts", contacts);
        return "admin/contact/contact-list";
    }

    @GetMapping("/contact/read/{id}")
    public String markAsRead(@PathVariable("id") Long id) {
        Contact contact = contactRepository.findById(id).orElse(null);
        if (contact != null) {
            contact.setStatus(1);
            contactRepository.save(contact);
        }
        return "redirect:/admin/contacts";
    }
}
