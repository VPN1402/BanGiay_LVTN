package com.example.LVTN.service.impl;

import com.example.LVTN.dto.ImportDetailUpdateDTO;
import com.example.LVTN.dto.WarehouseCheckDTO;
import com.example.LVTN.entity.ImportReceipt;
import com.example.LVTN.entity.ImportReceiptDetail;
import com.example.LVTN.entity.ProcurementRequest;
import com.example.LVTN.entity.ProductSize;
import com.example.LVTN.repository.ImportReceiptDetailRepository;
import com.example.LVTN.repository.ImportReceiptRepository;
import com.example.LVTN.repository.ProcurementRequestRepository;
import com.example.LVTN.repository.ProductSizeRepository;
import com.example.LVTN.service.ImportReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ImportReceiptServiceImpl implements ImportReceiptService {

    @Autowired
    private ImportReceiptRepository importReceiptRepository;

    @Autowired
    private ImportReceiptDetailRepository importReceiptDetailRepository;

    @Autowired
    private ProductSizeRepository productSizeRepository;

    @Autowired
    private ProcurementRequestRepository procurementRequestRepository;

    // BƯỚC 1: XỬ LÝ LƯU NHÁP PHIẾU TỪ FILE CSV
    @Override
    public void saveDraftReceipt(ImportReceipt receipt) {
        BigDecimal total = BigDecimal.ZERO;

        // Lưu phiếu nhập trước để lấy ID sinh tự động cho các detail liên kết
        importReceiptRepository.save(receipt);

        if (receipt.getDetails() != null) {
            for (ImportReceiptDetail detail : receipt.getDetails()) {
                // Tính toán tổng tiền nháp ban đầu
                BigDecimal subtotal = detail.getImportPrice().multiply(new BigDecimal(detail.getQuantity()));
                total = total.add(subtotal);

                importReceiptDetailRepository.save(detail);
            }
        }

        receipt.setTotalAmount(total);
        importReceiptRepository.save(receipt);
    }

    // BƯỚC 2: ADMIN DUYỆT VÀ TỰ DO HẠ SỐ LƯỢNG (CHẶN KHÔNG CHO PHÉP TĂNG VƯỢT CSV)
    @Override
    public void adminApproveReceipt(Long id, List<ImportDetailUpdateDTO> decisions) {
        // 1. Lấy phiếu từ DB
        ImportReceipt receipt = importReceiptRepository.findById(id).orElseThrow();

        // 2. Lặp qua danh sách quyết định của Admin
        for (ImportDetailUpdateDTO decision : decisions) {
            ImportReceiptDetail detail = importReceiptDetailRepository.findById(decision.getDetailId()).orElse(null);

            if (detail != null) {
                // Gán dữ liệu
                detail.setApprovedQuantity(decision.getApprovedQty());
                detail.setIsApproved(true); // Quan trọng: Phải set true thì Thủ kho mới thấy

                // LƯU TRỰC TIẾP CÁI CHI TIẾT NÀY VÀO DB
                importReceiptDetailRepository.save(detail);
            }
        }

        // 3. Cập nhật trạng thái phiếu
        receipt.setStatus("APPROVED");
        importReceiptRepository.save(receipt);
    }
    // BƯỚC 3: THỦ KHO KIỂM ĐẾM THỰC TẾ (BỔ SUNG LOGIC CHẶN SAI LỆCH TOÁN HỌC)
    @Override
    public void warehouseConfirmAndImportStock(Long id, List<WarehouseCheckDTO> checkResults) {
        ImportReceipt receipt = importReceiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập"));

        BigDecimal finalTotalAmount = BigDecimal.ZERO;

        for (WarehouseCheckDTO result : checkResults) {
            ImportReceiptDetail detail = importReceiptDetailRepository.findById(result.getDetailId()).orElse(null);

            if (detail != null && Boolean.TRUE.equals(detail.getIsApproved())) {

                // ================= CHẶN LOGIC BẢO MẬT Ở BACKEND =================
                int totalCountByWarehouse = result.getActualQty() + result.getDamagedQty();
                if (totalCountByWarehouse != detail.getApprovedQuantity()) {
                    throw new RuntimeException("Lỗi nhập liệu sản phẩm '"
                            + detail.getProductSize().getProduct().getName()
                            + "': Tổng số lượng thực nhận (" + result.getActualQty()
                            + ") và số lượng lỗi (" + result.getDamagedQty()
                            + ") phải bằng đúng số lượng sếp duyệt (" + detail.getApprovedQuantity() + " đôi)!");
                }
                // ================================================================

                // Nếu hợp lệ, tiến hành lưu vào DB
                detail.setActualQuantity(result.getActualQty());
                detail.setDamagedQuantity(result.getDamagedQty());
                detail.setQuantity(result.getActualQty()); // Chỉ cộng số lượng hàng TỐT vào kho

                // Thực hiện cộng dồn số hàng tốt vào tồn kho
                ProductSize ps = productSizeRepository.findById(detail.getProductSize().getId()).orElse(null);
                if (ps != null) {
                    ps.setQuantity(ps.getQuantity() + result.getActualQty());
                    productSizeRepository.save(ps);
                }

                // Tính tổng tiền hóa đơn thực tế cuối cùng dựa trên số hàng tốt nhận được
                BigDecimal subtotal = detail.getImportPrice().multiply(new BigDecimal(result.getActualQty()));
                finalTotalAmount = finalTotalAmount.add(subtotal);

                importReceiptDetailRepository.save(detail);
            }
        }

        receipt.setTotalAmount(finalTotalAmount);
        receipt.setStatus("COMPLETED");
        importReceiptRepository.save(receipt);
    }

    @Override
    public List<ImportReceipt> findAll() {
        return importReceiptRepository.findAll();
    }

    @Override
    public ImportReceipt findById(Long id) {
        return importReceiptRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void selectWinningBid(Long procurementRequestId, Long winningReceiptId) {
        // 1. Lấy tất cả các bảng báo giá nộp cho đợt gọi hàng này
        List<ImportReceipt> allBids = importReceiptRepository.findByProcurementRequestId(procurementRequestId);

        for (ImportReceipt bid : allBids) {
            if (bid.getId().equals(winningReceiptId)) {
                // Phiếu được Admin chọn -> Chuyển trạng thái để Thủ kho thấy
                bid.setStatus("APPROVED");
            } else {
                // Các phiếu của nhà cung cấp khác bị loại
                bid.setStatus("REJECTED");
            }
            importReceiptRepository.save(bid);
        }

        // 2. Đóng đợt gọi hàng lại, không nhận thêm báo giá nữa
        ProcurementRequest request = procurementRequestRepository.findById(procurementRequestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt gọi hàng"));
        request.setStatus("CLOSED");
        procurementRequestRepository.save(request);
    }
    @Override
    @Transactional
    public ImportReceipt save(ImportReceipt receipt) {
        // Tự động tính toán tổng tiền lô hàng dựa trên số lượng và giá thực tế trước khi lưu
        if (receipt.getDetails() != null) {
            BigDecimal total = BigDecimal.ZERO;
            for (ImportReceiptDetail detail : receipt.getDetails()) {
                // Nếu trạng thái hoàn tất (COMPLETED), tính theo số lượng thực nhận (actualQuantity)
                if ("COMPLETED".equals(receipt.getStatus())) {
                    if (detail.getActualQuantity() != null && detail.getImportPrice() != null) {
                        BigDecimal itemTotal = detail.getImportPrice().multiply(new BigDecimal(detail.getActualQuantity()));
                        total = total.add(itemTotal);
                    }
                } else {
                    // Nếu đang ở bước duyệt, tính tạm thời theo số lượng duyệt mua (approvedQuantity)
                    if (detail.getApprovedQuantity() != null && detail.getImportPrice() != null) {
                        BigDecimal itemTotal = detail.getImportPrice().multiply(new BigDecimal(detail.getApprovedQuantity()));
                        total = total.add(itemTotal);
                    }
                }
            }
            receipt.setTotalAmount(total);
        }

        // Gọi xuống Repository để lưu chính thức xuống Database
        return importReceiptRepository.save(receipt);
    }
}