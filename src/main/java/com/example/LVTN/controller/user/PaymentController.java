package com.example.LVTN.controller.user;

import com.example.LVTN.entity.Order;
import com.example.LVTN.security.VNPayConfig;
import com.example.LVTN.service.EmailService;
import com.example.LVTN.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private EmailService emailService;

    @Value("${vnp.tmn.code}") private String vnp_TmnCode;
    @Value("${vnp.hash.secret}") private String vnp_HashSecret;
    @Value("${vnp.pay.url}") private String vnp_PayUrl;
    @Value("${vnp.return.url}") private String vnp_ReturnUrl;

    @GetMapping("/vnpay/create")
    public String createPaymentUrl(@RequestParam("orderId") Long orderId,
                                   HttpServletRequest request) throws Exception {

        Order order = orderService.findById(orderId);

        if (order == null) {
            return "redirect:/checkout?error=order_not_found";
        }

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = String.valueOf(order.getId());
        String vnp_OrderInfo = "Thanh toan don hang " + order.getId();
        String vnp_OrderType = "other";
        String vnp_Locale = "vn";

        long amount = order.getFinalTotal()
                .multiply(new BigDecimal("100"))
                .longValue();

        Map<String, String> vnp_Params = new HashMap<>();

        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);

        vnp_Params.put("vnp_Locale", vnp_Locale);
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);

        vnp_Params.put("vnp_IpAddr", VNPayConfig.getIpAddress(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15);

        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {

            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);

            if (fieldValue != null && !fieldValue.isEmpty()) {

                hashData.append(fieldName)
                        .append("=")
                        .append(URLEncoder.encode(
                                fieldValue,
                                StandardCharsets.US_ASCII.toString()
                        ));

                query.append(URLEncoder.encode(
                        fieldName,
                        StandardCharsets.US_ASCII.toString()
                ));

                query.append("=");

                query.append(URLEncoder.encode(
                        fieldValue,
                        StandardCharsets.US_ASCII.toString()
                ));

                if (itr.hasNext()) {
                    hashData.append("&");
                    query.append("&");
                }
            }
        }

        String vnp_SecureHash = VNPayConfig.hmacSHA512(
                vnp_HashSecret,
                hashData.toString()
        );

        query.append("&vnp_SecureHash=");
        query.append(vnp_SecureHash);

        String paymentUrl = vnp_PayUrl + "?" + query;

        return "redirect:" + paymentUrl;
    }

    @GetMapping("/vnpay/return")
    public String paymentReturn(HttpServletRequest request) {
        String orderIdStr = request.getParameter("vnp_TxnRef");
        String responseCode = request.getParameter("vnp_ResponseCode");
        try {
            Long orderId = Long.parseLong(orderIdStr);
            if ("00".equals(responseCode)) {
                // 1. Cập nhật trạng thái thanh toán thành công xuống Database
                orderService.updatePaymentStatus(orderId, "PAID");

                // =========================================================================
                // ĐOẠN THÊM MỚI: TIẾN HÀNH GỬI EMAIL HÓA ĐƠN CHI TIẾT KHI VNPAY THÀNH CÔNG
                // =========================================================================
                try {
                    // Lấy ra thông tin đơn hàng đầy đủ từ database sau khi đã update trạng thái PAID
                    Order completedOrder = orderService.findById(orderId);
                    if (completedOrder != null) {
                        // Truyền nguyên đối tượng Order vào để Service tự động bóc tách và vẽ bảng sản phẩm HTML
                        emailService.sendOrderConfirmationEmail(completedOrder);
                    }
                } catch (Exception mailException) {
                    // Bao bọc try-catch riêng để nếu mạng nghẽn không gửi được mail,
                    // khách vẫn được chuyển hướng sang trang success bình thường, không bị đứng web.
                    System.err.println(">>> Lỗi hệ thống gửi mail (VNPAY): " + mailException.getMessage());
                }
                // =========================================================================

                request.getSession().removeAttribute("cart");
                return "redirect:/checkout/success?orderId=" + orderId;
            } else {
                orderService.updatePaymentStatus(orderId, "FAILED");
                return "redirect:/checkout?error=payment_failed";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/checkout?error=system_error";
        }
    }

}