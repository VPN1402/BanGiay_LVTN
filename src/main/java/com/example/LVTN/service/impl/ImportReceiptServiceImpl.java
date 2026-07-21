package com.example.LVTN.service.impl;

import com.example.LVTN.dto.ImportDetailUpdateDTO;
import com.example.LVTN.dto.WarehouseCheckDTO;
import com.example.LVTN.entity.ImportReceipt;
import com.example.LVTN.entity.ImportReceiptDetail;
import com.example.LVTN.entity.ProcurementRequest;
import com.example.LVTN.entity.ProductSize;
import com.example.LVTN.entity.User;
import com.example.LVTN.repository.ImportReceiptDetailRepository;
import com.example.LVTN.repository.ImportReceiptRepository;
import com.example.LVTN.repository.ProcurementRequestRepository;
import com.example.LVTN.repository.ProductSizeRepository;
import com.example.LVTN.service.ActivityLogService;
import com.example.LVTN.service.ImportReceiptService;
import com.example.LVTN.utils.SecurityUtils;
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

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private SecurityUtils securityUtils; // Inject SecurityUtils vào đây

    // BƯỚC 1: XỬ LÝ LƯU NHÁP PHIẾU TỪ FILE CSV
    @Override
    public void saveDraftReceipt(ImportReceipt receipt) {
        BigDecimal total = BigDecimal.ZERO;

        importReceiptRepository.save(receipt);

        if (receipt.getDetails() != null) {
            for (ImportReceiptDetail detail : receipt.getDetails()) {
                BigDecimal subtotal = detail.getImportPrice().multiply(new BigDecimal(detail.getQuantity()));
                total = total.add(subtotal);
                importReceiptDetailRepository.save(detail);
            }
        }

        receipt.setTotalAmount(total);
        importReceiptRepository.save(receipt);

        User currentUser = securityUtils.getCurrentLoggedInUser();
        Long userId = (currentUser != null) ? currentUser.getId() : null;
        String fullName = (currentUser != null && currentUser.getFullName() != null) ? currentUser.getFullName() : "Hệ thống / NCC";
        String roleName = securityUtils.getCurrentRoleName();

        activityLogService.log(
                userId,
                fullName,
                roleName,
                "NỘP BÁO GIÁ NHẬP HÀNG",
                fullName + " đã tiếp nhận/tạo phiếu báo giá nháp #" + receipt.getId() + " - Dự kiến tổng tiền: " + total + " VNĐ"
        );
    }

    // BƯỚC 2: ADMIN DUYỆT VÀ CHÍNH THỨC XÁC THỰC SỐ LƯỢNG MUA
    @Override
    public void adminApproveReceipt(Long id, List<ImportDetailUpdateDTO> decisions) {
        ImportReceipt receipt = importReceiptRepository.findById(id).orElseThrow();

        for (ImportDetailUpdateDTO decision : decisions) {
            ImportReceiptDetail detail = importReceiptDetailRepository.findById(decision.getDetailId()).orElse(null);

            if (detail != null) {
                detail.setApprovedQuantity(decision.getApprovedQty());
                detail.setIsApproved(true);
                importReceiptDetailRepository.save(detail);
            }
        }

        receipt.setStatus("APPROVED");
        importReceiptRepository.save(receipt);

        User currentUser = securityUtils.getCurrentLoggedInUser();
        Long userId = (currentUser != null) ? currentUser.getId() : null;
        String fullName = (currentUser != null && currentUser.getFullName() != null) ? currentUser.getFullName() : "Admin";
        String roleName = securityUtils.getCurrentRoleName();

        activityLogService.log(
                userId,
                fullName,
                roleName,
                "XÁC THỰC DUYỆT PHIẾU NHẬP",
                fullName + " đã phê duyệt số lượng và chấp thuận phiếu nhập hàng #" + receipt.getId() + " - Chờ thủ kho kiểm nhận."
        );
    }

    // BƯỚC 3: THỦ KHO KIỂM ĐẾM THỰC TẾ VÀ ĐƯA VÀO KHO (ĐÃ FIX LƯU LOG CHUẨN XÁC)
    @Override
    public void warehouseConfirmAndImportStock(Long id, List<WarehouseCheckDTO> checkResults) {
        ImportReceipt receipt = importReceiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập"));

        BigDecimal finalTotalAmount = BigDecimal.ZERO;

        for (WarehouseCheckDTO result : checkResults) {
            ImportReceiptDetail detail = importReceiptDetailRepository.findById(result.getDetailId()).orElse(null);

            if (detail != null && Boolean.TRUE.equals(detail.getIsApproved())) {
                int totalCountByWarehouse = result.getActualQty() + result.getDamagedQty();
                if (totalCountByWarehouse != detail.getApprovedQuantity()) {
                    throw new RuntimeException("Lỗi nhập liệu sản phẩm '"
                            + detail.getProductSize().getProduct().getName()
                            + "': Tổng số lượng thực nhận (" + result.getActualQty()
                            + ") và số lượng lỗi (" + result.getDamagedQty()
                            + ") phải bằng đúng số lượng sếp duyệt (" + detail.getApprovedQuantity() + " đôi)!");
                }

                detail.setActualQuantity(result.getActualQty());
                detail.setDamagedQuantity(result.getDamagedQty());
                detail.setQuantity(result.getActualQty());

                ProductSize ps = productSizeRepository.findById(detail.getProductSize().getId()).orElse(null);
                if (ps != null) {
                    ps.setQuantity(ps.getQuantity() + result.getActualQty());
                    productSizeRepository.save(ps);
                }

                BigDecimal subtotal = detail.getImportPrice().multiply(new BigDecimal(result.getActualQty()));
                finalTotalAmount = finalTotalAmount.add(subtotal);

                importReceiptDetailRepository.save(detail);
            }
        }

        receipt.setTotalAmount(finalTotalAmount);
        receipt.setStatus("COMPLETED");
        importReceiptRepository.save(receipt);

        // ================= LOGGING DÀNH RIÊNG CHO THỦ KHO =================
        User currentUser = securityUtils.getCurrentLoggedInUser();
        Long userId = (currentUser != null) ? currentUser.getId() : null;
        String fullName = (currentUser != null && currentUser.getFullName() != null) ? currentUser.getFullName() : "Thủ kho";
        String roleName = securityUtils.getCurrentRoleName();

        activityLogService.log(
                userId,
                fullName,
                roleName,
                "HOÀN TẤT KIỂM NHẬN KHO",
                fullName + " đã thực tế kiểm nhận & nhập kho thành công cho phiếu #" + receipt.getId() + " - Giá trị nhập: " + finalTotalAmount + " VNĐ"
        );
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
        List<ImportReceipt> allBids = importReceiptRepository.findByProcurementRequestId(procurementRequestId);

        for (ImportReceipt bid : allBids) {
            if (bid.getId().equals(winningReceiptId)) {
                bid.setStatus("APPROVED");
            } else {
                bid.setStatus("REJECTED");
            }
            importReceiptRepository.save(bid);
        }

        ProcurementRequest request = procurementRequestRepository.findById(procurementRequestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt gọi hàng"));
        request.setStatus("CLOSED");
        procurementRequestRepository.save(request);

        User currentUser = securityUtils.getCurrentLoggedInUser();
        Long userId = (currentUser != null) ? currentUser.getId() : null;
        String fullName = (currentUser != null && currentUser.getFullName() != null) ? currentUser.getFullName() : "Admin / CEO";
        String roleName = securityUtils.getCurrentRoleName();

        activityLogService.log(
                userId,
                fullName,
                roleName,
                "CHỌN BÁO GIÁ TRÚNG THẦU",
                fullName + " đã phê duyệt phiếu báo giá #" + winningReceiptId + " trúng thầu cho Đợt gọi hàng #" + procurementRequestId
        );
    }

    @Override
    @Transactional
    public ImportReceipt save(ImportReceipt receipt) {
        if (receipt.getDetails() != null) {
            BigDecimal total = BigDecimal.ZERO;
            for (ImportReceiptDetail detail : receipt.getDetails()) {
                if ("COMPLETED".equals(receipt.getStatus())) {
                    if (detail.getActualQuantity() != null && detail.getImportPrice() != null) {
                        BigDecimal itemTotal = detail.getImportPrice().multiply(new BigDecimal(detail.getActualQuantity()));
                        total = total.add(itemTotal);
                    }
                } else {
                    if (detail.getApprovedQuantity() != null && detail.getImportPrice() != null) {
                        BigDecimal itemTotal = detail.getImportPrice().multiply(new BigDecimal(detail.getApprovedQuantity()));
                        total = total.add(itemTotal);
                    }
                }
            }
            receipt.setTotalAmount(total);
        }

        return importReceiptRepository.save(receipt);
    }
}