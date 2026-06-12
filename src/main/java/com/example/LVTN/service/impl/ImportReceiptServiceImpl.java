package com.example.LVTN.service.impl;

import com.example.LVTN.dto.ImportDetailUpdateDTO;
import com.example.LVTN.dto.WarehouseCheckDTO;
import com.example.LVTN.entity.ImportReceipt;
import com.example.LVTN.entity.ImportReceiptDetail;
import com.example.LVTN.entity.ProductSize;
import com.example.LVTN.repository.ImportReceiptDetailRepository;
import com.example.LVTN.repository.ImportReceiptRepository;
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
        // Tìm phiếu nhập từ database, nếu không thấy sẽ báo lỗi ngay
        ImportReceipt receipt = importReceiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập"));

        BigDecimal totalAmountAfterApprove = BigDecimal.ZERO;

        for (ImportDetailUpdateDTO decision : decisions) {
            ImportReceiptDetail detail = importReceiptDetailRepository.findById(decision.getDetailId()).orElse(null);

            if (detail != null) {
                // Cập nhật trạng thái xem Admin có tích chọn duyệt mặt hàng này không
                detail.setIsApproved(decision.getIsSelected());

                if (decision.getIsSelected()) {

                    // ================= CHẶN BẢO MẬT Ở BACKEND =================
                    // Nếu số lượng gõ vào lớn hơn số lượng ban đầu đọc từ file CSV
                    if (decision.getApprovedQty() > detail.getRequestedQuantity()) {
                        throw new RuntimeException("Lỗi bảo mật: Số lượng phê duyệt cho sản phẩm '"
                                + detail.getProductSize().getProduct().getName()
                                + "' (" + decision.getApprovedQty() + " đôi) "
                                + "không được phép vượt quá số lượng đề xuất trong file CSV ("
                                + detail.getRequestedQuantity() + " đôi)!");
                    }
                    // ==========================================================

                    // Nếu dữ liệu hợp lệ (nhỏ hơn hoặc bằng số lượng CSV) -> Tiến hành gán số lượng
                    detail.setApprovedQuantity(decision.getApprovedQty());
                    detail.setQuantity(decision.getApprovedQty());

                    // Tính lại tiền dựa trên số lượng vừa được chốt mua thực tế
                    BigDecimal subtotal = detail.getImportPrice().multiply(new BigDecimal(decision.getApprovedQty()));
                    totalAmountAfterApprove = totalAmountAfterApprove.add(subtotal);
                } else {
                    // Nếu gạch bỏ không chọn duyệt mã hàng này
                    detail.setApprovedQuantity(0);
                    detail.setQuantity(0);
                }
                importReceiptDetailRepository.save(detail);
            }
        }

        // Cập nhật lại tổng tiền mới sau khi đã hạ bớt số lượng hàng ế và chuyển sang chờ thủ kho kiểm đếm
        receipt.setTotalAmount(totalAmountAfterApprove);
        receipt.setStatus("APPROVED");
        importReceiptRepository.save(receipt);
    }

    // BƯỚC 3: THỦ KHO KIỂM ĐẾM THỰC TẾ VÀ CHỐT CỘNG TỒN KHO
    @Override
    public void warehouseConfirmAndImportStock(Long id, List<WarehouseCheckDTO> checkResults) {
        // ĐÃ SỬA: Thay thế 'orElsethrows' thành 'orElseThrow' chuẩn Java
        ImportReceipt receipt = importReceiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập"));

        BigDecimal finalTotalAmount = BigDecimal.ZERO;

        for (WarehouseCheckDTO result : checkResults) {
            ImportReceiptDetail detail = importReceiptDetailRepository.findById(result.getDetailId()).orElse(null);

            if (detail != null && Boolean.TRUE.equals(detail.getIsApproved())) {
                detail.setActualQuantity(result.getActualQty());
                detail.setDamagedQuantity(result.getDamagedQty());
                detail.setQuantity(result.getActualQty());

                // Thực hiện cộng dồn trực tiếp vào số lượng tồn kho của Size sản phẩm đó
                ProductSize ps = productSizeRepository.findById(detail.getProductSize().getId()).orElse(null);
                if (ps != null) {
                    ps.setQuantity(ps.getQuantity() + result.getActualQty());
                    productSizeRepository.save(ps);
                }

                // Tính tổng tiền hóa đơn thực tế cuối cùng
                BigDecimal subtotal = detail.getImportPrice().multiply(new BigDecimal(result.getActualQty()));
                finalTotalAmount = finalTotalAmount.add(subtotal);

                importReceiptDetailRepository.save(detail);
            }
        }

        receipt.setTotalAmount(finalTotalAmount);
        receipt.setStatus("COMPLETED"); // Chu trình kết thúc vĩnh viễn
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
}