package com.example.LVTN.controller.user;


import com.example.LVTN.entity.Product;
import com.example.LVTN.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/product-list")
    public String listProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            Model model, HttpServletRequest request) {

        List<Product> products = productService.filterProducts(categoryId, minPrice, maxPrice);

        model.addAttribute("products", products);
        model.addAttribute("currentUri", request.getRequestURI());

        model.addAttribute("selectedCat", categoryId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        return "user/product/product-list";
    }
    @GetMapping("/product/detail/{id}")
    public String productDetail(@PathVariable("id") Long id, Model model, HttpServletRequest request) {

        Product product = productService.findById(id);

        if (product == null) {
            return "redirect:user/product/list";
        }

        model.addAttribute("product", product);
        model.addAttribute("currentUri", request.getRequestURI());

        return "user/product/product-detail";
    }
}