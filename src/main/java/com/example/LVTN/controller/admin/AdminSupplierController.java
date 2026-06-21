package com.example.LVTN.controller.admin;
import com.example.LVTN.entity.Role;
import com.example.LVTN.entity.Supplier;
import com.example.LVTN.entity.User;
import com.example.LVTN.repository.RoleRepository;
import com.example.LVTN.repository.SupplierRepository;
import com.example.LVTN.repository.UserRepository;
import com.example.LVTN.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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

    // --- SUPPLIERS ---
    @GetMapping("/suppliers")
    public String listSuppliers(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        return "admin/supplier/list";
    }

    @GetMapping("/suppliers/add")
    public String showAddSupplierForm() {
        return "admin/supplier/add"; // Trỏ đúng đến file HTML vừa tạo ở Bước 1
    }

    // 2. Xử lý đồng thời: Lưu Nhà cung cấp + Tạo & liên kết luôn Tài khoản
    @PostMapping("/supplier/save") // Đảm bảo có dấu gạch chéo / ở đầu
    public String saveSupplier(
            @RequestParam("name") String name,
            @RequestParam(value = "supplierPhone", required = false) String supplierPhone,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam(value = "userPhone", required = false) String userPhone,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes) {

        // Làm sạch dữ liệu email đầu vào để tránh khoảng trắng vô tình
        String cleanEmail = (email != null) ? email.trim() : "";

        try {
            // 1. Kiểm tra chính xác email sạch
            if (!cleanEmail.isEmpty()) {
                User existingUser = userRepository.findByEmail(cleanEmail).orElse(null);
                if (existingUser != null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Thất bại: Email [" + cleanEmail + "] đã được cấp cho tài khoản khác!");
                    return "redirect:/admin/suppliers/add"; // Đồng bộ chuẩn đường dẫn GET hiển thị form
                }
            }

            // BƯỚC A: Khởi tạo và lưu thông tin Nhà Cung Cấp mới
            Supplier supplier = new Supplier();
            supplier.setName(name);
            supplier.setPhone(supplierPhone);
            supplier.setAddress(address);
            supplier = supplierRepository.save(supplier);

            // BƯỚC B: Khởi tạo Tài khoản đăng nhập gán trực tiếp cho Nhà Cung Cấp này
            User user = new User();
            user.setFullName(fullName);
            user.setEmail(cleanEmail);
            user.setPhone(userPhone);
            user.setStatus(1);

            // Mã hóa mật khẩu bảo mật theo cơ chế Spring Security
            user.setPassword(passwordEncoder.encode(password));

            // Tìm và gán quyền nhà cung cấp ROLE_SUPPLIER
            Role supplierRole = roleRepository.findByRoleName("ROLE_SUPPLIER");
            if (supplierRole == null) {
                supplierRole = new Role();
                supplierRole.setRoleName("ROLE_SUPPLIER");
                roleRepository.save(supplierRole);
            }
            user.setRole(supplierRole);

            // LIÊN KẾT: Đưa thực thể Supplier vừa lưu vào tài khoản
            user.setSupplier(supplier);

            // Lưu tài khoản đăng nhập xuống database
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm nhà cung cấp thành công và cấp tài khoản: " + cleanEmail);
            return "redirect:/admin/suppliers/add"; // Đồng bộ chuẩn 100%

        } catch (Exception e) {
            // In chi tiết lỗi thực sự ra console (màn hình đen) để bạn nhìn thấy lỗi thật là gì
            e.printStackTrace();

            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi hệ thống xảy ra: " + e.getMessage());
            return "redirect:/admin/suppliers/add"; // 🛠️ ĐÃ SỬA: Không bao giờ sợ nhảy nhầm đường dẫn lạ gây lỗi ảo nữa
        }
    }

    @GetMapping("/supplier/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Supplier supplier = supplierService.findById(id);
        model.addAttribute("supplier", supplier);
        return "admin/supplier/update";
    }

    @GetMapping("/supplier/delete/{id}")
    public String deleteSupplier(@PathVariable("id") Long id,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (supplierService.hasReceipts(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa đối tác này vì hệ thống đang lưu lịch sử các lô hàng giày thể thao do họ cung cấp!");
        } else {
            supplierService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thông tin nhà cung cấp khỏi danh sách!");
        }
        return "redirect:/admin/suppliers";
    }


}