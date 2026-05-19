package com.example.LVTN.controller.admin;

import com.example.LVTN.entity.ImportReceipt;
import com.example.LVTN.entity.ImportReceiptDetail;
import com.example.LVTN.entity.User;
import com.example.LVTN.repository.ProductSizeRepository;
import com.example.LVTN.repository.SupplierRepository;
import com.example.LVTN.service.ImportReceiptService;
import com.example.LVTN.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminImportController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductSizeRepository productSizeRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ImportReceiptService importReceiptService;

    @GetMapping("/import/list")
    public String listImports(Model model) {
        model.addAttribute("receipts", importReceiptService.findAll());
        return "admin/import/import-list";
    }

    @GetMapping("/import/create")
    public String showCreateForm(Model model) {
        model.addAttribute("importReceipt", new ImportReceipt());
        model.addAttribute("allProductSizes", productSizeRepository.findAll());
        model.addAttribute("allSuppliers", supplierRepository.findAll());
        return "admin/import/import-stock";
    }

    @PostMapping("/import/save")
    public String saveImport(@ModelAttribute("importReceipt") ImportReceipt receipt, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        receipt.setUser(user);

        if (receipt.getDetails() != null) {
            for (ImportReceiptDetail detail : receipt.getDetails()) {
                detail.setImportReceipt(receipt);
            }
        }
        importReceiptService.saveReceipt(receipt);
        return "redirect:/admin/import/list";
    }
}