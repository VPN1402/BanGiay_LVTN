package com.example.LVTN.controller.admin;

import com.example.LVTN.entity.Product;
import com.example.LVTN.entity.ProductSize;
import com.example.LVTN.repository.ProductSizeRepository;
import com.example.LVTN.service.BrandService;
import com.example.LVTN.service.CategoryService;
import com.example.LVTN.service.ProductService;
import com.example.LVTN.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private ProductSizeRepository productSizeRepository;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("totalProducts", productService.findAll().size());
        model.addAttribute("totalCategories", categoryService.findAll().size());
        model.addAttribute("totalUsers", userService.findAll().size());
        model.addAttribute("totalBrands", brandService.findAll().size());
        model.addAttribute("products", productService.findAll());
        model.addAttribute("users", userService.findAll());
        return "admin/dashboard/dashboard";
    }

    @GetMapping("/inventory")
    public String showInventory(Model model) {
        List<Product> products = productService.findAll();
        long sapHetCount = products.stream().filter(p -> p.getTotalQuantity() > 0 && p.getTotalQuantity() <= 10).count();
        long hetHangCount = products.stream().filter(p -> p.getTotalQuantity() == 0).count();

        model.addAttribute("products", products);
        model.addAttribute("sapHetCount", sapHetCount);
        model.addAttribute("hetHangCount", hetHangCount);
        return "admin/inventory/inventory";
    }

    @PostMapping("/inventory/update")
    public String updateInventory(@RequestParam("productSizeIds") List<Long> sizeIds,
                                  @RequestParam("quantities") List<Integer> quantities) {
        if (sizeIds != null && quantities != null) {
            for (int i = 0; i < sizeIds.size(); i++) {
                Long id = sizeIds.get(i);
                Integer qty = quantities.get(i);
                ProductSize ps = productSizeRepository.findById(id).orElse(null);
                if (ps != null) {
                    ps.setQuantity(qty != null ? qty : 0);
                    productSizeRepository.save(ps);
                }
            }
        }
        return "redirect:/admin/inventory";
    }
}