package com.example.LVTN.controller.user;

import com.example.LVTN.entity.Product;
import com.example.LVTN.repository.ProductRepository;
import com.example.LVTN.service.BrandService;
import com.example.LVTN.service.CategoryService;
import com.example.LVTN.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @GetMapping("/product-list")
    public String listProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model, HttpServletRequest request) {

        Double minPrice = null;
        Double maxPrice = null;

        if (priceRange != null && !priceRange.isEmpty()) {
            switch (priceRange) {
                case "under-1m":
                    maxPrice = 1000000.0;
                    break;
                case "under-2m":
                    maxPrice = 2000000.0;
                    break;
                case "under-3m":
                    maxPrice = 3000000.0;
                    break;
                case "1m-3m":
                    minPrice = 1000000.0;
                    maxPrice = 3000000.0;
                    break;
                case "above-3m":
                    minPrice = 3000000.0;
                    break;
            }
        }

        Pageable pageable = PageRequest.of(page, 9);


        Page<Product> productPage = productService.filterProducts(categoryId, brandId, minPrice, maxPrice, size, keyword, pageable);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());

        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("brands", brandService.findAll());


        model.addAttribute("selectedCat", categoryId);
        model.addAttribute("selectedBrand", brandId);
        model.addAttribute("selectedSize", size);
        model.addAttribute("priceRange", priceRange);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentUri", request.getRequestURI());

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