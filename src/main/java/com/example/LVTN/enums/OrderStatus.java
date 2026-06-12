package com.example.LVTN.enums;

public enum OrderStatus {
    PENDING,        // Chờ xác nhận
    CONFIRMED,      // Đã xác nhận
    SHIPPING,       // Đang giao hàng
    DELIVERED,      // Đã giao thành công
    CANCELLED,      // Đã hủy
    OUT_OF_STOCK    // ⚠️ THÊM MỚI: Hết hàng trong kho (Chờ NCC hoặc thương lượng với khách)
}