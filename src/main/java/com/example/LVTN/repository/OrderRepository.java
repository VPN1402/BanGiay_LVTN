package com.example.LVTN.repository;

import com.example.LVTN.entity.Order;
import com.example.LVTN.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByUser(User user);
    List<Order> findByOrderStatusOrderByPriorityAscCreatedAtAsc(String orderStatus);

    // 1. Tính tổng doanh thu từ các đơn hàng GIAO THÀNH CÔNG (Dùng finalTotal)
    @Query("SELECT COALESCE(SUM(o.finalTotal), 0) FROM Order o WHERE o.orderStatus = 'DELIVERED'")
    BigDecimal calculateTotalRevenue();

    // 2. Tính tổng doanh thu thuần (ĐÃ SỬA: Thêm COALESCE để không bao giờ bị NULL)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus = 'DELIVERED'")
    BigDecimal calculateNetRevenue();

    // 3. Đếm tổng số đơn hàng
    @Query("SELECT COUNT(o) FROM Order o")
    Long countTotalOrders();

    // 4. Đếm số đơn hàng bị hủy
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus = 'CANCELLED'")
    Long countCancelledOrders();

    // 5. Thống kê doanh thu theo từng tháng trong năm nay (Native Query phục vụ biểu đồ)
    @Query(value = "SELECT DATE_FORMAT(o.created_at, 'Tháng %m') as monthLabel, " +
            "       IFNULL(SUM(o.total_amount), 0) / 1000000 as totalMillions " +
            "FROM orders o " +
            "WHERE o.order_status = 'DELIVERED' AND YEAR(o.created_at) = YEAR(CURDATE()) " +
            "GROUP BY DATE_FORMAT(o.created_at, 'Tháng %m') " +
            "ORDER BY MONTH(o.created_at) ASC", nativeQuery = true)
    List<Object[]> getMonthlyRevenueTrend();
}