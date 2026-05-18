package com.example.LVTN.controller;

import com.example.LVTN.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private CartService cartService;


    @ModelAttribute("totalQuantity")
    public int getTotalQuantity() {
        try {
            return cartService.getTotalQuantity();
        } catch (Exception e) {
            return 0;
        }
    }
}