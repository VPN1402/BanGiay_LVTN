package com.example.LVTN.controller.admin;

import com.example.LVTN.entity.User;
import com.example.LVTN.service.RoleService;
import com.example.LVTN.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public String manageUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/user/list";
    }

    @GetMapping("/user/add")
    public String addUser(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", roleService.findAll());
        return "admin/user/add";
    }

    @PostMapping("/user/save")
    public String saveUser(@ModelAttribute("user") User formUser) {
        if (formUser.getId() != null && formUser.getId() > 0) {

            User existingUser = userService.findById(formUser.getId());
            if (existingUser != null) {

                existingUser.setFullName(formUser.getFullName());
                existingUser.setEmail(formUser.getEmail());
                existingUser.setPhone(formUser.getPhone());
                existingUser.setStatus(formUser.getStatus());
                existingUser.setRole(formUser.getRole());


                String roleName = existingUser.getRole() != null ? existingUser.getRole().getRoleName().toLowerCase() : "";
                boolean isStaff = roleName.contains("nhân viên") || roleName.contains("thủ kho") || roleName.contains("cửa hàng trưởng");

                if (isStaff && formUser.getPassword() != null && !formUser.getPassword().trim().isEmpty()) {
                    existingUser.setPassword(passwordEncoder.encode(formUser.getPassword()));
                }



                userService.save(existingUser);
            }
        } else {

            if (formUser.getPassword() != null && !formUser.getPassword().isEmpty()) {
                formUser.setPassword(passwordEncoder.encode(formUser.getPassword()));
            }
            userService.save(formUser);
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/user/edit/{id}")
    public String editUser(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        model.addAttribute("user", user);
        model.addAttribute("roles", roleService.findAll());
        return "admin/user/update";
    }

    @GetMapping("/user/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return "redirect:/admin/users";
    }
}