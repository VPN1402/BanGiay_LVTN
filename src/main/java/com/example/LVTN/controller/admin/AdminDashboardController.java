package com.example.LVTN.controller.admin;

import com.example.LVTN.dto.ProductSaleDTO;
import com.example.LVTN.entity.Product;
import com.example.LVTN.entity.ProductSize;
import com.example.LVTN.repository.OrderRepository;
import com.example.LVTN.repository.ProductRepository;
import com.example.LVTN.repository.ProductSizeRepository;
import com.example.LVTN.service.BrandService;
import com.example.LVTN.service.CategoryService;
import com.example.LVTN.service.ProductService;
import com.example.LVTN.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        // 1. GOM CÁC CHỈ SỐ TỔNG QUAN (METRICS)
        long totalProducts = productRepository.count();
        int totalStock = productSizeRepository.calculateTotalStock();

        List<ProductSize> lowStockItems = productSizeRepository.findAllLowStock();
        int lowStockCount = lowStockItems.size();

        long slowProductsCount = productRepository.countProductsWithZeroSales();
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();

        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalStock", totalStock);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("slowProductsCount", slowProductsCount);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("lowStockItems", lowStockItems);

        // 2. LẤY DANH SÁCH TOP ĐỂ HIỂN THỊ BẢNG
        List<ProductSaleDTO> topSelling = productRepository.getTopSellingProducts(PageRequest.of(0, 5)).getContent();
        List<ProductSaleDTO> topSlow = productRepository.getSlowSellingProducts(PageRequest.of(0, 5)).getContent();

        model.addAttribute("topSellingProducts", topSelling);
        model.addAttribute("topSlowProducts", topSlow);

        // ==========================================
        // 3. NGHIỆP VỤ NÂNG CẤP: DỮ LIỆU ĐỔ VÀO BIỂU ĐỒ (CHARTS)
        // ==========================================

        // Dữ liệu cho BIỂU ĐỒ CỘT (Top 5 sản phẩm bán chạy)
        List<String> barChartLabels = topSelling.stream()
                .map(ProductSaleDTO::getProductName)
                .collect(Collectors.toList());
        List<Long> barChartData = topSelling.stream()
                .map(ProductSaleDTO::getTotalSold)
                .collect(Collectors.toList());

        model.addAttribute("barChartLabels", barChartLabels);
        model.addAttribute("barChartData", barChartData);

        // Dữ liệu cho BIỂU ĐỒ TRÒN (Phân tích cấu trúc hàng tồn kho)
        long totalSizeRecords = productSizeRepository.count(); // Tổng số bản ghi size trong kho
        long safeStockCount = totalSizeRecords - lowStockCount; // Các vị trí kho đang ở mức an toàn

        model.addAttribute("pieChartLabels", List.of("Tồn kho an toàn", "Báo động sắp hết/Hết hàng"));
        model.addAttribute("pieChartData", List.of(safeStockCount, lowStockCount));

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