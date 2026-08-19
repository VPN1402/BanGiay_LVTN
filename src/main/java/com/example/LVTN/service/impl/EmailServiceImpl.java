package com.example.LVTN.service.impl;

import com.example.LVTN.entity.Order;
import com.example.LVTN.entity.OrderItem;
import com.example.LVTN.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    @Async
    public void sendOrderConfirmationEmail(Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Định dạng tiền tệ VND cho đẹp
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

            String toEmail = order.getUser().getEmail();
            String customerName = order.getReceiverName() != null ? order.getReceiverName() : order.getUser().getEmail();

            helper.setFrom("phuocnhancvcv248@gmail.com", "Xưởng Giày Thể Thao Shoe Store");
            helper.setTo(toEmail);
            helper.setSubject("👟 Xác nhận đơn hàng thành công tại Shoe Store Shop #" + order.getId());

            // 1. Tự động chuyển đổi hiển thị Trạng thái thanh toán
            String paymentStatusText = "Chưa thanh toán (Thu hộ COD)";
            if ("PAID".equalsIgnoreCase(order.getPaymentStatus()) || "REFUNDED".equalsIgnoreCase(order.getPaymentStatus())) {
                paymentStatusText = "Đã thanh toán trực tuyến";
            }

            // 2. Tự động duyệt vòng lặp danh sách sản phẩm để tạo các dòng <tr> trong HTML
            StringBuilder productRows = new StringBuilder();
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    // Giả định OrderItem của bạn có: .getProduct().getName(), .getQuantity(), .getPrice()
                    String productName = (item.getProduct() != null) ? item.getProduct().getName() : "Sản phẩm giày";
                    int quantity = item.getQuantity();
                    String priceFormatted = currencyFormatter.format(item.getPrice());
                    String subTotalFormatted = currencyFormatter.format(item.getPrice().multiply(new java.math.BigDecimal(quantity)));

                    productRows.append("<tr style='border-bottom: 1px solid #eee;'>")
                            .append("<td style='padding: 10px; font-size: 14px;'>").append(productName).append("</td>")
                            .append("<td style='padding: 10px; text-align: center; font-size: 14px;'>").append(quantity).append("</td>")
                            .append("<td style='padding: 10px; text-align: right; font-size: 14px;'>").append(priceFormatted).append("</td>")
                            .append("<td style='padding: 10px; text-align: right; font-size: 14px; font-weight: bold;'>").append(subTotalFormatted).append("</td>")
                            .append("</tr>");
                }
            }


            java.math.BigDecimal shippingFee = new java.math.BigDecimal("30000");


            java.math.BigDecimal finalTotal = order.getTotalAmount().add(shippingFee);

            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 650px; margin: auto; padding: 25px; border: 1px solid #e0e0e0; border-radius: 12px; box-shadow: 0 4px 8px rgba(0,0,0,0.05);'>"
                    + "<div style='text-align: center; margin-bottom: 20px;'>"
                    + "  <h2 style='color: #2c3e50; margin-bottom: 5px;'>CẢM ƠN BẠN ĐÃ ĐẶT HÀNG!</h2>"
                    + "  <p style='color: #7f8c8d; margin-top: 0;'>Đơn hàng của bạn đang được hệ thống xử lý</p>"
                    + "</div>"
                    + "<p>Xin chào <b>" + customerName + "</b>,</p>"
                    + "<p>Hệ thống đã ghi nhận thông tin đặt hàng thành công của bạn với thông tin chi tiết dưới đây:</p>"
                    + "<hr style='border: none; border-top: 1px solid #eee; margin: 15px 0;'>"

                    // Bảng Thông tin giao hàng & Thanh toán
                    + "<table style='width: 100%; font-size: 14px; margin-bottom: 20px; line-height: 1.6;'>"
                    + "  <tr><td style='width: 35%; color: #666;'><b>Mã đơn hàng:</b></td><td><b>#" + order.getId() + "</b></td></tr>"
                    + "  <tr><td style='color: #666;'><b>Người nhận hàng:</b></td><td>" + order.getReceiverName() + " (" + order.getReceiverPhone() + ")</td></tr>"
                    + "  <tr><td style='color: #666;'><b>Địa chỉ giao hàng:</b></td><td>" + order.getShippingAddress() + "</td></tr>"
                    + "  <tr><td style='color: #666;'><b>Hình thức thanh toán:</b></td><td>" + order.getPaymentMethod() + "</td></tr>"
                    + "  <tr><td style='color: #666;'><b>Trạng thái:</b></td><td><span style='background-color: #e3f2fd; color: #0d47a1; padding: 3px 8px; border-radius: 4px; font-size: 12px; font-weight: bold;'>" + paymentStatusText + "</span></td></tr>"
                    + "</table>"

                    // Bảng chi tiết giỏ hàng món đồ
                    + "<table style='width: 100%; border-collapse: collapse; margin: 20px 0;'>"
                    + "  <tr style='background-color: #f8f9fa; border-bottom: 2px solid #dee2e6; font-size: 14px;'>"
                    + "    <th style='padding: 10px; text-align: left;'>Tên Sản Phẩm</th>"
                    + "    <th style='padding: 10px; text-align: center;'>SL</th>"
                    + "    <th style='padding: 10px; text-align: right;'>Đơn Giá</th>"
                    + "    <th style='padding: 10px; text-align: right;'>Tổng</th>"
                    + "  </tr>"
                    + productRows.toString() // Nhúng danh sách sản phẩm đã duyệt ở trên vào đây
                    + "</table>"

                    // Tổng tiền cuối cùng
                    + "<div style='text-align: right; margin-top: 15px; font-size: 15px;'>"
                    + "  <div style='margin-bottom: 8px;'>"
                    + "    <span style='color: #666;'>Tạm tính tiền hàng: </span>"
                    + "    <span style='font-weight: bold;'>" + currencyFormatter.format(order.getTotalAmount()) + "</span>"
                    + "  </div>"
                    + "  <div style='margin-bottom: 8px;'>"
                    + "    <span style='color: #666;'>Phí vận chuyển: </span>"
                    + "    <span style='font-weight: bold;'>30.000 ₫</span>"
                    + "  </div>"
                    + "  <div style='border-top: 1px solid #eee; padding-top: 10px;'>"
                    + "    <b>Tổng cộng tiền thanh toán: </b>"
                    + "    <span style='color: #dc3545; font-weight: bold; font-size: 20px;'>"
                    + currencyFormatter.format(finalTotal)
                    + "    </span>"
                    + "  </div>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println(">>> Đã gửi email hóa đơn chi tiết thành công tới: " + toEmail);

        } catch (Exception e) {
            System.err.println(">>> Thất bại khi gửi email hóa đơn: " + e.getMessage());
            e.printStackTrace();
        }
    }
}