package com.example.LVTN.controller.user;

import com.example.LVTN.dto.CheckoutRequest;
import com.example.LVTN.entity.Order;
import com.example.LVTN.entity.User;
import com.example.LVTN.service.CartService;
import com.example.LVTN.service.UserService; // Đảm bảo đã import interface chuẩn
import com.example.LVTN.service.impl.OrderServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller // Thêm annotation này nếu class của bạn đang thiếu
public class OrderController {

    @Autowired
    private OrderServiceImpl orderService;

    // BƯỚC 1: Tiêm đối tượng userService vào đây (chữ u viết thường)
    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    @GetMapping("/checkout")
    public String showCheckoutPage(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login"; // Chưa đăng nhập thì đá về trang login
        }

        // 1. Gửi thông tin giỏ hàng qua Model để hiển thị tóm tắt hóa đơn bên cánh phải
        model.addAttribute("cartItems", cartService.getCartForUser().getItems());
        model.addAttribute("totalPrice", cartService.getTotalPrice());

        // 2. NGHIỆP VỤ QUAN TRỌNG: Gửi một object CheckoutRequest rỗng sang HTML để binding form
        model.addAttribute("checkoutRequest", new CheckoutRequest());

        return "user/order/checkout";
    }

    @PostMapping("/checkout/process")
    public String processCheckout(@ModelAttribute CheckoutRequest checkoutRequest,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        try {
            // Trường hợp chưa đăng nhập (Principal null) để bảo vệ hệ thống tránh lỗi dữ liệu
            if (principal == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập trước khi thực hiện thanh toán!");
                return "redirect:/login";
            }

            // BƯỚC 2: Sửa "UserService" (Tên class) thành "userService" (Tên biến được autowired)
            User currentUser = userService.findByEmail(principal.getName());

            // Gọi Service xử lý Đặt hàng
            Order newOrder = orderService.placeOrder(currentUser, checkoutRequest);

            // Phân luồng Thanh Toán
            if ("VNPAY".equals(checkoutRequest.getPaymentMethod())) {
                return "redirect:/payment/vnpay/create?orderId=" + newOrder.getId();
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
        return "user/order/success"; // Trỏ tới file success.html
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

        // Sửa chỗ này: so sánh email thay vì full name
        // Giả sử user trong Order có trường email hoặc bạn lấy từ chính principal
        if (order != null && order.getUser().getEmail().equals(principal.getName())) {

            if ("SHIPPING".equals(order.getOrderStatus())) {
                orderService.updateOrderStatus(orderId, "DELIVERED");
            }
        }

        return "redirect:/order-history";
    }
}