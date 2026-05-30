package com.example.LVTN.service;

import com.example.LVTN.dto.CheckoutRequest;
import com.example.LVTN.entity.Order;
import com.example.LVTN.entity.User;

import java.util.List;

public interface OrderService {
    List<Order> findAll();
    Order placeOrder(User currentUser, CheckoutRequest request);
    Order findById(Long id);
    public void updatePaymentStatus(Long orderId, String status);
    List<Order> getOrdersByUserId(Long userId);
    void updateOrderDetails(Long orderId, String orderStatus, String paymentStatus);
    void updateOrderStatus(Long orderId, String newStatus);
}
