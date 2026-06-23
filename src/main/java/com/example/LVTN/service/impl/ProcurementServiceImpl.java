package com.example.LVTN.service.impl;

import com.example.LVTN.entity.*;
import com.example.LVTN.repository.*;
import com.example.LVTN.service.ProcurementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service // Bắt buộc phải có để Controller có thể @Autowired Service này
public class ProcurementServiceImpl implements ProcurementService {

    @Autowired
    private ProcurementRequestRepository procRequestRepo;

    @Autowired
    private ProcurementRequestDetailRepository procDetailRepo;

    @Autowired
    private ProductSizeRepository productSizeRepository; // Đã thêm

    @Override
    @Transactional
    public void generateRequestIfNeeded(String note) {
        // 1. Quét tìm danh sách các sản phẩm/size đang dưới ngưỡng an toàn
        List<ProductSize> lowStockSizes = productSizeRepository.findAllLowStock();

        // Nếu kho vẫn đầy đủ, không thiếu món nào thì dừng lại luôn
        if (lowStockSizes.isEmpty()) {
            return;
        }

        // 2. Khởi tạo "Khung" Đợt thu mua (Bảng mẹ)
        ProcurementRequest request = new ProcurementRequest();
        request.setStatus("OPEN");

        if (note != null && !note.trim().isEmpty()) {
            request.setNote(note);
        } else {
            request.setNote("Hệ thống tự động khởi tạo do phát hiện thiếu hụt tồn kho.");
        }

        // LƯU TRƯỚC request để Database sinh ra ID, lát nữa mới gán vào bảng con được
        request = procRequestRepo.save(request);

        // 3. VÒNG LẶP QUAN TRỌNG: Bỏ từng sản phẩm thiếu vào đợt thu mua (Bảng con)
        for (ProductSize ps : lowStockSizes) {
            ProcurementRequestDetail detail = new ProcurementRequestDetail();
            detail.setProcurementRequest(request); // Nối bản ghi con này vào đợt hàng vừa tạo
            detail.setProductSize(ps);             // Xác định sản phẩm và size cụ thể

            // Tính toán số lượng Admin cần mua bổ sung
            // Công thức: Số lượng cần mua = Ngưỡng tối thiểu (min) - Tồn kho thực tế (current)
            int neededQty = ps.getMinQuantity() - ps.getQuantity();

            // Phòng trường hợp lỗi logic âm, thiết lập mặc định tối thiểu là 10 cái
            if (neededQty <= 0) {
                neededQty = 10;
            }

            detail.setQuantityNeeded(neededQty);

            // Lưu dòng chi tiết này vào Database
            procDetailRepo.save(detail);
        }
    }
    @Override
    public ProcurementRequest getDetailById(Long id) {
        return procRequestRepo.findById(id).orElse(null);
    }
}