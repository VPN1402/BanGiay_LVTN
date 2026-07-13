package com.example.LVTN.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSaleDTO {
    private Long productId;
    private String productName;
    private Long totalSold;     // Tổng số lượng đã bán (kiểu Long do SQL SUM trả về)
    private Long currentStock;  // Tổng tồn kho hiện tại (cộng tất cả các size)
}