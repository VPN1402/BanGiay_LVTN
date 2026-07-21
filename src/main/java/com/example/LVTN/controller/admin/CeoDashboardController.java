package com.example.LVTN.controller.admin;

import com.example.LVTN.entity.ActivityLog;
import com.example.LVTN.repository.ActivityLogRepository;
import com.example.LVTN.repository.ImportReceiptRepository;
import com.example.LVTN.repository.OrderRepository;
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
    private ImportReceiptRepository importReceiptRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @GetMapping("/dashboard")
    public String showCeoDashboard(Model model) {

        // 1. Tính Tổng Vốn Đầu Tư (Tổng tiền nhập hàng)
        BigDecimal totalInvestment = importReceiptRepository.calculateTotalExpense();
        if (totalInvestment == null) totalInvestment = BigDecimal.ZERO;

        // 2. Lấy Doanh Thu Thuần
        BigDecimal netRevenue = orderRepository.calculateNetRevenue();
        if (netRevenue == null) netRevenue = BigDecimal.ZERO;

        // 3. Tính Lời / Lỗ
        BigDecimal profitOrLoss = netRevenue.subtract(totalInvestment);

        // 4. Lấy 20 lịch sử thao tác gần nhất
        List<ActivityLog> recentActivities = activityLogRepository.findTop20ByOrderByCreatedAtDesc();

        // Truyền dữ liệu ra View HTML
        model.addAttribute("totalInvestment", totalInvestment);
        model.addAttribute("totalRevenue", netRevenue);
        model.addAttribute("profitOrLoss", profitOrLoss);
        model.addAttribute("recentActivities", recentActivities);

        // 5. Xử lý dữ liệu Biểu đồ Cột (Doanh thu theo tháng)
        List<Object[]> rawTrendData = orderRepository.getMonthlyRevenueTrend();
        List<String> trendLabels = new ArrayList<>();
        List<Double> trendData = new ArrayList<>();

        if (rawTrendData != null) {
            for (Object[] row : rawTrendData) {
                if (row != null && row.length >= 2) {
                    trendLabels.add(row[0] != null ? row[0].toString() : "");
                    trendData.add(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
                }
            }
        }

        model.addAttribute("trendLabels", trendLabels);
        model.addAttribute("trendData", trendData);

        return "admin/ceo/dashboard";
    }
}