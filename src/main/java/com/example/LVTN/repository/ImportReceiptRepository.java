package com.example.LVTN.repository;

import com.example.LVTN.entity.ImportReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface ImportReceiptRepository extends JpaRepository<ImportReceipt, Long> {
    //tìm ra all file csv nộp chung cho 1 đợt gọi hàng
    List<ImportReceipt> findByProcurementRequestId(Long procurementRequestId);
    @Query("""
   SELECT COALESCE(SUM(i.totalAmount), 0)
   FROM ImportReceipt i
   WHERE i.status = 'COMPLETED'
   """)
    BigDecimal calculateTotalExpense();
}
