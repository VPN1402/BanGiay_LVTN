package com.example.LVTN.repository;

import com.example.LVTN.entity.Order;
import com.example.LVTN.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByUser(User user);

    List<Order> findByOrderStatusOrderByPriorityAscCreatedAtAsc(String orderStatus);

    // Tính tổng doanh thu từ các đơn hàng GIAO THÀNH CÔNG
    @Query("SELECT COALESCE(SUM(o.finalTotal), 0) FROM Order o WHERE o.orderStatus = 'DELIVERED'")
    java.math.BigDecimal calculateTotalRevenue();

    // 1. Tính tổng doanh thu thuần (Chỉ tính đơn hàng đã Giao thành công)
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.orderStatus = 'DELIVERED'")
    BigDecimal calculateNetRevenue();

    // 2. Đếm tổng số đơn hàng để tính tỷ lệ hủy
    @Query("SELECT COUNT(o) FROM Order o")
    Long countTotalOrders();

    // 3. Đếm số đơn hàng bị hủy
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = 'CANCELLED'")
    Long countCancelledOrders();

    // 4. Thống kê doanh thu theo từng tháng trong năm nay (Native Query phục vụ biểu đồ đường)
    @Query(value = "SELECT DATE_FORMAT(o.created_at, 'Tháng %m') as monthLabel, " +
            "       IFNULL(SUM(o.total_amount), 0) / 1000000 as totalMillions " +
            "FROM orders o " +
            "WHERE o.order_status = 'DELIVERED' AND YEAR(o.created_at) = YEAR(CURDATE()) " +
            "GROUP BY DATE_FORMAT(o.created_at, 'Tháng %m') " +
            "ORDER BY MONTH(o.created_at) ASC", nativeQuery = true)
    List<Object[]> getMonthlyRevenueTrend();
}
