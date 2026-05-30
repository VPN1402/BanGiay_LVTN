package com.example.LVTN.controller.user;

import com.example.LVTN.entity.Product;
import com.example.LVTN.service.CategoryService;
import com.example.LVTN.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class CategoryController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/category")
    public String getAllCategories(Model model, HttpServletRequest request) {
        model.addAttribute("categories", categoryService.findAll());

        model.addAttribute("currentUri", request.getRequestURI());

        return "user/category/category-list";
    }

    @GetMapping("/category/{id}")
    public String getProductByCategory(@PathVariable Long id, Model model, HttpServletRequest request) {
        
        List<Product> products = productService.filterProducts(id, null, null, null);

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCat", id);

        model.addAttribute("currentUri", request.getRequestURI());

        return "user/product/product-list";
    }
}