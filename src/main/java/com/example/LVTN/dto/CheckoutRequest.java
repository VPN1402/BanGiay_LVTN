package com.example.LVTN.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CheckoutRequest {
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private String note;
    private String paymentMethod; // "COD" hoặc "VNPAY"
}