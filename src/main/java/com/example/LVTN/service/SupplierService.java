package com.example.LVTN.service;

import com.example.LVTN.entity.Supplier;
import java.util.List;

public interface SupplierService {

    List<Supplier> findAll();

    Supplier findById(Long id);

    void save(Supplier supplier);

    void delete(Long id);

    // Kiểm tra xem nhà cung cấp này đã từng giao lô hàng nào chưa (tránh lỗi khóa ngoại)
    boolean hasReceipts(Long id);
}