package com.example.LVTN.controller.user;

import com.example.LVTN.dto.CheckoutRequest;
import com.example.LVTN.entity.Order;
import com.example.LVTN.entity.User;
import com.example.LVTN.service.CartService;
import com.example.LVTN.service.EmailService;
import com.example.LVTN.service.UserService; // Đảm bảo đã import interface chuẩn
import com.example.LVTN.service.impl.OrderServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class OrderController {

    @Autowired
    private OrderServiceImpl orderService;


    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/checkout")
    public String showCheckoutPage(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        // lấy tt giỏ
        model.addAttribute("cartItems", cartService.getCartForUser().getItems());
        model.addAttribute("totalPrice", cartService.getTotalPrice());

        model.addAttribute("checkoutRequest", new CheckoutRequest());

        return "user/order/checkout";
    }

    @PostMapping("/checkout/process")
    public String processCheckout(@ModelAttribute CheckoutRequest checkoutRequest,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        try {

            if (principal == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập trước khi thực hiện thanh toán!");
                return "redirect:/login";
            }

            User currentUser = userService.findByEmail(principal.getName());

            Order newOrder = orderService.placeOrder(currentUser, checkoutRequest);

            // Phân luồng Thanh Toán
            if ("VNPAY".equals(checkoutRequest.getPaymentMethod())) {
                return "redirect:/payment/vnpay/create?orderId=" + newOrder.getId();
            }

            try {
                // Chỉ cần truyền duy nhất thực thể newOrder vào, mọi việc bóc tách thông tin cứ để EmailService lo
                emailService.sendOrderConfirmationEmail(newOrder);
            } catch (Exception mailException) {
                System.err.println(">>> Lỗi hệ thống gửi mail hóa đơn (COD): " + mailException.getMessage());
            }


            // Nếu là COD, báo thành công
            redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn của bạn là: #" + newOrder.getId());
            return "redirect:/checkout/success";

        } catch (RuntimeException e) {
            // Lỗi hết hàng hoặc giỏ hàng trống sẽ văng ra đây
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/checkout/success")
    public String showSuccessPage() {
        return "user/order/success";
    }


    @GetMapping("/order-history")
    public String showOrderHistory(Model model) {
        User user = cartService.getLoggedInUser();
        if (user == null) return "redirect:/login";

        model.addAttribute("orders", orderService.getOrdersByUserId(user.getId()));
        return "user/order/order-history";
    }

    @PostMapping("/order/confirm-received")
    public String confirmReceived(@RequestParam Long orderId, Principal principal) {
        if (principal == null) return "redirect:/login";

        Order order = orderService.findById(orderId);



        if (order != null && order.getUser().getEmail().equals(principal.getName())) {

            if ("SHIPPING".equals(order.getOrderStatus())) {
                orderService.updateOrderStatus(orderId, "DELIVERED");
            }
        }

        return "redirect:/order-history";
    }
    @GetMapping("/order-details/{id}")
    public String showOrderDetails(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        Order order = orderService.findById(id);

        if (order == null || !order.getUser().getEmail().equals(principal.getName())) {
            return "redirect:/order-history";
        }

        model.addAttribute("order", order);
        return "user/order/order-details";
    }

    @PostMapping("/order/cancel")
    public String cancelOrder(@RequestParam Long orderId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByEmail(principal.getName());
            orderService.cancelOrder(orderId, user.getId());
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/order-history";
    }

    @PostMapping("/admin/orders/confirm-refund")
    public String confirmRefund(@RequestParam("orderId") Long orderId, RedirectAttributes redirectAttributes) {
        try {

            orderService.updateOrderDetails(orderId, "CANCELLED", "REFUNDED");

            redirectAttributes.addFlashAttribute("success", "Xác nhận đã hoàn tiền thành công cho đơn hàng #" + orderId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }
}