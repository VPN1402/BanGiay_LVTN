package com.example.LVTN.controller.admin;

import com.example.LVTN.entity.Order;
import com.example.LVTN.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    
    @GetMapping
    public String listOrders(Model model) {

        model.addAttribute("orders", orderService.findAll());
        return "admin/Order/order-list";
    }

    // Xử lý cập nhật trạng thái
    @PostMapping("/update")
    public String updateOrder(@RequestParam Long orderId,
                              @RequestParam String orderStatus,
                              @RequestParam String paymentStatus) {
        orderService.updateOrderDetails(orderId, orderStatus, paymentStatus);
        return "redirect:/admin/orders?success";
    }
    @GetMapping("/details/{id}")
    public String orderDetails(@PathVariable Long id, Model model) {
        Order order = orderService.findById(id); // Bạn cần hàm findById
        model.addAttribute("order", order);
        return "admin/Order/order-details";
    }
}