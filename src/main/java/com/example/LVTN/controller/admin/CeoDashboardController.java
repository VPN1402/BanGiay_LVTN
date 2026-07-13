package com.example.LVTN.controller.admin;

import com.example.LVTN.repository.OrderRepository;
import com.example.LVTN.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/ceo")
public class CeoDashboardController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/dashboard")
    public String showCeoDashboard(Model model) {

        // 1. Lấy các chỉ số tài chính vĩ mô từ Database
        BigDecimal netRevenue = orderRepository.calculateNetRevenue();
        BigDecimal deadCapital = productRepository.calculateDeadCapital();

        Long totalOrders = orderRepository.countTotalOrders();
        Long cancelledOrders = orderRepository.countCancelledOrders();

        // Tính toán tỷ lệ hủy đơn an toàn
        double returnRate = 0.0;
        if (totalOrders != null && totalOrders > 0 && cancelledOrders != null) {
            returnRate = ((double) cancelledOrders / totalOrders) * 100;
        }

        // Đẩy số liệu ra giao diện (Bảo vệ trường hợp DB trống bằng cách check null)
        model.addAttribute("netRevenue", netRevenue != null ? netRevenue : BigDecimal.ZERO);
        model.addAttribute("deadCapital", deadCapital != null ? deadCapital : BigDecimal.ZERO);
        model.addAttribute("returnRate", String.format("%.1f%%", returnRate));
        model.addAttribute("totalBranches", 1); // Giả lập hệ thống có 1 trung tâm tổng kho lớn nhất

        // 2. Xử lý dữ liệu Biểu đồ đường (Doanh thu xu hướng các tháng)
        List<Object[]> rawTrendData = orderRepository.getMonthlyRevenueTrend();
        List<String> trendLabels = new ArrayList<>();
        List<Double> trendData = new ArrayList<>();

        for (Object[] row : rawTrendData) {
            trendLabels.add(row[0].toString()); // Ví dụ: "Tháng 01"
            trendData.add(((Number) row[1]).doubleValue()); // Số tiền (triệu đồng)
        }
        model.addAttribute("trendLabels", trendLabels);
        model.addAttribute("trendData", trendData);

        // 3. Xử lý dữ liệu Biểu đồ ngang (Thị phần doanh số theo hãng giày)
        List<Object[]> rawBrandData = productRepository.getRevenuePercentageByBrand();
        List<String> brandLabels = new ArrayList<>();
        List<Double> brandData = new ArrayList<>();

        for (Object[] row : rawBrandData) {
            brandLabels.add(row[0].toString()); // Ví dụ: "Nike"
            brandData.add(((Number) row[1]).doubleValue()); // Tỷ lệ %
        }
        model.addAttribute("brandLabels", brandLabels);
        model.addAttribute("brandData", brandData);

        return "admin/ceo/dashboard";
    }
}