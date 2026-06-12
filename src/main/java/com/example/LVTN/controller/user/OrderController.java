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
import java.util.List;

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

    // Khúc này trong OrderController.java sửa lại như sau để hết sạch lỗi:
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

            // Trong hàm placeOrder bên trên đã tự động tính toán gán độ ưu tiên (Priority) rồi, không cần gọi lưu thủ công ở đây nữa!
            Order newOrder = orderService.placeOrder(currentUser, checkoutRequest);

            // Phân luồng Thanh Toán
            if ("VNPAY".equals(checkoutRequest.getPaymentMethod())) {
                return "redirect:/payment/vnpay/create?orderId=" + newOrder.getId();
            }

            try {
                emailService.sendOrderConfirmationEmail(newOrder);
            } catch (Exception mailException) {
                System.err.println(">>> Lỗi hệ thống gửi mail hóa đơn (COD): " + mailException.getMessage());
            }

            // Nếu là COD, báo thành công
            redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn của bạn là: #" + newOrder.getId());
            return "redirect:/checkout/success";

        } catch (RuntimeException e) {
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
        // 1. Lấy Email của người đang đăng nhập từ Spring Security
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();

        // 2. Tìm đối tượng User đầy đủ dưới Database bằng Email
        User user = userService.findByEmail(currentEmail);

        // 3. NẠP ĐỐI TƯỢNG USER VÀO MODEL (Bước quyết định)
        model.addAttribute("user", user);

        // 4. Lấy danh sách đơn hàng (Code cũ của bạn)
        List<Order> orders = orderService.findByUser(user);
        model.addAttribute("orders", orders);

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
    // THÊM VÀO CUỐI FILE ORDERCONTROLLER.JAVA CỦA BẠN

    @PostMapping("/admin/warehouse/submit-export/{orderId}")
    public String submitExportWarehouse(@PathVariable Long orderId,
                                        Principal principal,
                                        RedirectAttributes redirectAttributes) {
        try {
            // 1. Kiểm tra nếu nhân viên chưa đăng nhập (hết hạn session)
            if (principal == null) {
                redirectAttributes.addFlashAttribute("error", "Phiên làm việc đã hết hạn, vui lòng đăng nhập lại!");
                return "redirect:/login";
            }

            // 2. Lấy tên tài khoản (Email/Username) của nhân viên/admin đang bấm nút thực hiện
            String usernameNhanVien = principal.getName();

            // 3. Gọi xuống tầng Service để thực hiện kiểm tra thực tế & trừ kho
            String result = orderService.submitXuatKho(orderId, usernameNhanVien);

            // 4. Phân loại kết quả trả về để hiển thị thông báo ra màn hình Admin
            if ("SUCCESS".equals(result)) {
                redirectAttributes.addFlashAttribute("success",
                        "📦 Xác nhận xuất kho thành công! Hệ thống đã tự động trừ kho thực tế và chuyển trạng thái đơn hàng thành [ĐANG GIAO].");
            } else if ("FAILED_OUT_OF_STOCK".equals(result)) {
                redirectAttributes.addFlashAttribute("error",
                        "⚠️ XUẤT KHO THẤT BẠI: Trong lúc chờ duyệt, sản phẩm thực tế trong kho đã bị hụt hoặc hết hàng! Đơn hàng đã tự động chuyển về trạng thái [Hết hàng kho].");
            }

        } catch (RuntimeException e) {
            // Hứng các lỗi runtime thông thường như không tìm thấy đơn, lỗi logic hệ thống...
            redirectAttributes.addFlashAttribute("error", "Lỗi xử lý: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống không xác định: " + e.getMessage());
        }

        // Sau khi xử lý xong, chuyển hướng quay trở lại đúng trang chi tiết đơn hàng đó để xem kết quả cập nhật
        return "redirect:/admin/orders/details/" + orderId;
    }

    // 1. Giao diện hiển thị khi khách quét mã QR trên hộp giày
    @GetMapping("/order/scan/{id}")
    public String scanQrOrderPage(@PathVariable Long id, Model model) {
        Order order = orderService.findById(id);
        if (order == null) {
            model.addAttribute("error", "Mã đơn hàng không tồn tại trên hệ thống!");
            return "user/order/scan-confirm";
        }
        model.addAttribute("order", order);
        return "user/order/scan-confirm"; // Tẹo nữa mình tạo file HTML này công phu lắm
    }

    // 2. Xử lý khi khách bấm nút "Xác nhận đã nhận hàng" trên điện thoại
    @PostMapping("/order/scan/{id}/confirm")
    public String processQrConfirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.confirmReceivedByQR(id);
            redirectAttributes.addFlashAttribute("success", "Xác nhận nhận hàng thành công! Kicks Store cảm ơn bạn.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/order/scan/" + id;
    }
}