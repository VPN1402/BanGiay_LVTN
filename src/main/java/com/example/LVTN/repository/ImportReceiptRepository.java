package com.example.LVTN.repository;

import com.example.LVTN.entity.ImportReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportReceiptRepository extends JpaRepository<ImportReceipt, Long> {
    //tìm ra all file csv nộp chung cho 1 đợt gọi hàng
    List<ImportReceipt> findByProcurementRequestId(Long procurementRequestId);
}
