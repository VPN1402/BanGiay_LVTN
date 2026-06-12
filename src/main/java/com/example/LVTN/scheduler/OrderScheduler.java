package com.example.LVTN.scheduler;

import com.example.LVTN.entity.Order;
import com.example.LVTN.enums.OrderStatus;
import com.example.LVTN.enums.PaymentStatus;
import com.example.LVTN.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderScheduler {

    @Autowired
    private OrderRepository orderRepository;

    // Cron tạo lịch chạy ngầm: Cứ đúng 1 giờ sáng mỗi ngày hệ thống sẽ tự động quét dọn database
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void autoConfirmLongShippingOrders() {
        System.out.println(">>> [HỆ THỐNG] Bắt đầu quét kiểm tra các đơn hàng giao quá 5 ngày...");

        // Tính mốc thời gian 5 ngày trước so với hiện tại
        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);

        // Tìm tất cả đơn hàng đang SHIPPING
        List<Order> shippingOrders = orderRepository.findAll().stream()
                .filter(o -> "SHIPPING".equals(o.getOrderStatus()) && o.getProcessedAt() != null)
                .toList();

        int count = 0;
        for (Order order : shippingOrders) {
            // Nếu thời gian xuất kho (processedAt) đã trôi qua hơn 5 ngày
            if (order.getProcessedAt().isBefore(fiveDaysAgo)) {
                order.setOrderStatus(OrderStatus.DELIVERED.name());

                if ("COD".equals(order.getPaymentMethod())) {
                    order.setPaymentStatus(PaymentStatus.PAID.name());
                }

                orderRepository.save(order);
                count++;
            }
        }
        System.out.println(">>> [HỆ THỐNG] Đã tự động hoàn thành thành công: " + count + " đơn hàng quá hạn 5 ngày.");
    }
}