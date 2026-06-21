package com.example.LVTN.service;

import com.example.LVTN.dto.ImportDetailUpdateDTO;
import com.example.LVTN.dto.WarehouseCheckDTO;
import com.example.LVTN.entity.ImportReceipt;
import java.util.List;

public interface ImportReceiptService {

    // BƯỚC 1: Lưu nháp phiếu từ file CSV
    void saveDraftReceipt(ImportReceipt receipt);

    // BƯỚC 2: Admin phê duyệt và hạ số lượng theo ý muốn
    void adminApproveReceipt(Long id, List<ImportDetailUpdateDTO> decisions);

    // BƯỚC 3: Thủ kho kiểm đếm thực tế và chốt cộng tồn kho
    void warehouseConfirmAndImportStock(Long id, List<WarehouseCheckDTO> checkResults);

    // Lấy danh sách lịch sử nhập kho
    List<ImportReceipt> findAll();

    // Lấy chi tiết một phiếu nhập
    ImportReceipt findById(Long id);

    public void selectWinningBid(Long procurementRequestId, Long winningReceiptId);

    ImportReceipt save(ImportReceipt receipt);
}