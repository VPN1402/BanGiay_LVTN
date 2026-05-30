package com.example.LVTN.service;

import com.example.LVTN.entity.Cart;
import com.example.LVTN.entity.User;
import java.math.BigDecimal;

public interface CartService {

    User getLoggedInUser();

    Cart getCartForUser();

    void add(Long productId, int size, int quantity);

    void update(Long productId, int size, int quantity);

    void remove(Long productId, int size);

    int getTotalQuantity();

    BigDecimal getTotalPrice();

    void clearCart(Long userId);
}