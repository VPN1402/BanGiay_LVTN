package com.example.LVTN.service;

import com.example.LVTN.dto.CheckoutRequest;
import com.example.LVTN.entity.Order;
import com.example.LVTN.entity.User;

import java.util.List;

public interface OrderService {
    List<Order> findAll();

    //hàm xly dh
    Order placeOrder(User currentUser, CheckoutRequest request);

    Order findById(Long id);

    void updatePaymentStatus(Long orderId, String status);

    List<Order> getOrdersByUserId(Long userId);

    //cap nhat ghang va tien`
    void updateOrderDetails(Long orderId, String orderStatus, String paymentStatus);

    void updateOrderStatus(Long orderId, String newStatus);

    void cancelOrder(Long orderId, Long userId);

    List<Order> findByUser(User user);
}
