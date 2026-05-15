package com.example.LVTN.controller;

import com.example.LVTN.entity.Product;
import com.example.LVTN.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest; // Thêm import này

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @GetMapping("/")
    public String home(Model model, HttpServletRequest request) {
        // 1. Lấy danh sách sản phẩm từ DB
        List<Product> products = productService.findAll();

        // 2. Đẩy danh sách vào model với tên là "products"
        model.addAttribute("products", products);

        // 3. Đẩy currentUri để Navbar không bị lỗi (như đã làm ở bước trước)
        model.addAttribute("currentUri", request.getRequestURI());

        return "home/index";
    }
}