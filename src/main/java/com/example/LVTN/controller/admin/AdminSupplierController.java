package com.example.LVTN.controller.admin;
import com.example.LVTN.entity.Supplier;
import com.example.LVTN.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminSupplierController {

    @Autowired
    private SupplierService supplierService;



    // --- SUPPLIERS ---
    @GetMapping("/suppliers")
    public String listSuppliers(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        return "admin/supplier/list";
    }

    @GetMapping("/supplier/add")
    public String showAddSupplier(Model model) {
        model.addAttribute("supplier", new Supplier());
        return "admin/supplier/add";
    }

    @PostMapping("/supplier/save")
    public String saveSupplier(@Valid @ModelAttribute("supplier") Supplier supplier,
                               BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // Nếu có lỗi, check xem là đang thêm mới hay sửa để trả về đúng file HTML tĩnh
            return supplier.getId() == null ? "admin/supplier/add" : "admin/supplier/update";
        }
        supplierService.save(supplier);
        return "redirect:/admin/suppliers";
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