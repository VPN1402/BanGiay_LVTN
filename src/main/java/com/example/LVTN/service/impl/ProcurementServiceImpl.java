package com.example.LVTN.service.impl;

import com.example.LVTN.entity.*;
import com.example.LVTN.repository.*;
import com.example.LVTN.service.ActivityLogService;
import com.example.LVTN.service.ProcurementService;
import com.example.LVTN.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProcurementServiceImpl implements ProcurementService {

    @Autowired
    private ProcurementRequestRepository procRequestRepo;

    @Autowired
    private ProcurementRequestDetailRepository procDetailRepo;

    @Autowired
    private ProductSizeRepository productSizeRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private SecurityUtils securityUtils; // Inject SecurityUtils dạng Bean chuẩn

    @Override
    @Transactional
    public void generateRequestIfNeeded(String note) {
        List<ProductSize> lowStockSizes = productSizeRepository.findAllLowStock();

        if (lowStockSizes.isEmpty()) {
            return;
        }

        ProcurementRequest request = new ProcurementRequest();
        request.setStatus("OPEN");

        if (note != null && !note.trim().isEmpty()) {
            request.setNote(note);
        } else {
            request.setNote("Hệ thống tự động khởi tạo do phát hiện thiếu hụt tồn kho.");
        }

        request = procRequestRepo.save(request);

        for (ProductSize ps : lowStockSizes) {
            ProcurementRequestDetail detail = new ProcurementRequestDetail();
            detail.setProcurementRequest(request);
            detail.setProductSize(ps);

            int neededQty = ps.getMinQuantity() - ps.getQuantity();
            if (neededQty <= 0) {
                neededQty = 10;
            }

            detail.setQuantityNeeded(neededQty);
            procDetailRepo.save(detail);
        }

        // ================= LOGGING ĐỘNG =================
        User currentUser = securityUtils.getCurrentLoggedInUser();
        Long userId = (currentUser != null) ? currentUser.getId() : null;
        String fullName = (currentUser != null && currentUser.getFullName() != null) ? currentUser.getFullName() : "Hệ thống";
        String roleName = securityUtils.getCurrentRoleName();

        activityLogService.log(
                userId,
                fullName,
                roleName,
                "TẠO ĐỢT THU MUA",
                fullName + " đã khởi tạo đợt thu mua hàng khẩn cấp #" + request.getId() + " - Ghi chú: " + request.getNote()
        );
    }

    @Override
    public ProcurementRequest getDetailById(Long id) {
        return procRequestRepo.findById(id).orElse(null);
    }
}