package com.example.LVTN.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class ImportUpdateForm {

    // 1. Dùng cho màn hình Admin (Ban quản lý duyệt mua)
    private List<ImportDetailUpdateDTO> adminDetails = new ArrayList<>();

    // 2. Dùng cho màn hình Thủ kho (Warehouse kiểm hàng tốt/lỗi)
    private List<WarehouseCheckDTO> warehouseDetails = new ArrayList<>();
}