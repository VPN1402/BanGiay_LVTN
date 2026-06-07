package com.example.LVTN.controller.user;

import com.example.LVTN.entity.Product;
import com.example.LVTN.service.CategoryService;
import com.example.LVTN.service.ProductService;
import com.example.LVTN.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.http.HttpServletRequest;


@Controller
public class CategoryController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @GetMapping("/category")
    public String getAllCategories(Model model, HttpServletRequest request) {
        model.addAttribute("products", productService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("currentUri", request.getRequestURI());

        return "user/category/category-list";
    }

    @GetMapping("/category/{id}")
    public String getProductByCategory(@PathVariable Long id, Model model, HttpServletRequest request) {


        Page<Product> productPage = productService.filterProducts(id, null, null, null, null, null, Pageable.unpaged());


        model.addAttribute("products", productPage.getContent());


        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", productPage.getTotalPages());


        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("brands", brandService.findAll());

        model.addAttribute("selectedCat", id);
        model.addAttribute("currentUri", request.getRequestURI());

        return "user/product/product-list";
    }
}