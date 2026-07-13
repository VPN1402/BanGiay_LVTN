package com.example.LVTN.controller.admin;

import com.example.LVTN.entity.Role;
import com.example.LVTN.entity.Supplier;
import com.example.LVTN.entity.User;
import com.example.LVTN.repository.RoleRepository;
import com.example.LVTN.repository.SupplierRepository;
import com.example.LVTN.repository.UserRepository;
import com.example.LVTN.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminSupplierController {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Hiển thị danh sách
    @GetMapping("/suppliers")
    public String listSuppliers(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        return "admin/supplier/list";
    }

    // Hiển thị Form thêm mới
    @GetMapping("/suppliers/add")
    public String showAddSupplierForm() {
        return "admin/supplier/add";
    }

    // Xử lý lưu form
    @PostMapping("/supplier/save")
    public String saveSupplier(
            @RequestParam("name") String name,
            @RequestParam(value = "supplierPhone", required = false) String supplierPhone,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam("fullName") String fullName,
            @RequestParam("supplierEmail") String supplierEmail,
            @RequestParam("userEmail") String userEmail,
            @RequestParam(value = "userPhone", required = false) String userPhone,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes) {


        String cleanUserEmail = (userEmail != null) ? userEmail.trim() : "";

        try {

            if (!cleanUserEmail.isEmpty()) {
                User existingUser = userRepository.findByEmail(cleanUserEmail).orElse(null);
                if (existingUser != null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Thất bại: Email đăng nhập [" + cleanUserEmail + "] đã được cấp cho tài khoản khác!");
                    return "redirect:/admin/suppliers/add";
                }
            }

            // Khởi tạo và lưu thông tin Nhà Cung Cấp mới
            Supplier supplier = new Supplier();
            supplier.setName(name);
            supplier.setEmail(supplierEmail.trim());
            supplier.setPhone(supplierPhone);
            supplier.setAddress(address);
            supplier = supplierRepository.save(supplier);

            // Khởi tạo Tài khoản đăng nhập cho Nhà Cung Cấp này
            User user = new User();
            user.setFullName(fullName);
            user.setEmail(cleanUserEmail);
            user.setPhone(userPhone);
            user.setStatus(1);


            user.setPassword(passwordEncoder.encode(password));

            // Tìm và gán quyền ROLE_SUPPLIER
            Role supplierRole = roleRepository.findByRoleName("ROLE_SUPPLIER");
            if (supplierRole == null) {
                supplierRole = new Role();
                supplierRole.setRoleName("ROLE_SUPPLIER");
                roleRepository.save(supplierRole);
            }
            user.setRole(supplierRole);


            user.setSupplier(supplier);


            userRepository.save(user);

            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm nhà cung cấp thành công và cấp tài khoản: " + cleanUserEmail);
            return "redirect:/admin/suppliers/add";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi hệ thống xảy ra: " + e.getMessage());
            return "redirect:/admin/suppliers/add";
        }
    }

    @GetMapping("/supplier/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Supplier supplier = supplierService.findById(id);
        model.addAttribute("supplier", supplier);
        return "admin/supplier/update";
    }

    @GetMapping("/supplier/delete/{id}")
    public String deleteSupplier(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        if (supplierService.hasReceipts(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa đối tác này vì hệ thống đang lưu lịch sử lô hàng!");
        } else {
            supplierService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thông tin nhà cung cấp khỏi danh sách!");
        }
        return "redirect:/admin/suppliers";
    }
}