package com.example.LVTN.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ImportDetailUpdateDTO {
    private Long detailId;
    private Boolean isSelected; // Admin tick chọn mua
    private Integer approvedQty; // Admin chốt số lượng
}