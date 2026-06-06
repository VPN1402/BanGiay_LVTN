package com.example.LVTN.service;

import com.example.LVTN.entity.Order;
import org.springframework.scheduling.annotation.Async;

public interface EmailService {
    @Async
    void sendOrderConfirmationEmail(Order order);
}
