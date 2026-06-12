package com.example.LVTN.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class WarehouseCheckDTO {
    private Long detailId;
    private Integer actualQty;   // Số lượng kho nhận (Hàng Tốt)
    private Integer damagedQty;  // Số lượng hàng rách hộp/hư hỏng
}