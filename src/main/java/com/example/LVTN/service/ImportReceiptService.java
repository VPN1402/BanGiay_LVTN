package com.example.LVTN.service;

import com.example.LVTN.entity.ImportReceipt;
import java.util.List;

public interface ImportReceiptService {

    void saveReceipt(ImportReceipt receipt);

    // Lấy danh sách lịch sử nhập kho
    List<ImportReceipt> findAll();

    // Lấy chi tiết một phiếu nhập
    ImportReceipt findById(Long id);
}