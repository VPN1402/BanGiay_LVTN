package com.example.LVTN.controller.admin;

import com.example.LVTN.dto.WarehouseCheckDTO;
import com.example.LVTN.entity.ImportReceipt;
import com.example.LVTN.entity.ImportReceiptDetail;
import com.example.LVTN.service.ImportReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/warehouse")
public class WarehouseController {

    @Autowired
    private ImportReceiptService importReceiptService;

    // 1. Màn hình danh sách đơn hàng đã được Admin duyệt mua, đang chờ thực nhận nhập kho
    @GetMapping("/pending")
    public String listPendingStock(Model model) {
        // Lọc danh sách hoặc lấy tất cả tùy thuộc logic Repo, ở đây tạm dùng findAll để hiển thị xử lý kho
        model.addAttribute("receipts", importReceiptService.findAll());
        return "warehouse/pending-list";
    }

    // 2. Xem chi tiết kiểm kho tốt/lỗi cho phiếu nhập
    @GetMapping("/check/{id}")
    public String showWarehouseCheckForm(@PathVariable("id") Long id, Model model) {
        ImportReceipt receipt = importReceiptService.findById(id);
        model.addAttribute("receipt", receipt);
        return "warehouse/check-form";
    }

    // 3. Thủ kho xác nhận số lượng thực tế kiểm đếm và cộng dồn vào hệ thống kho giày
    @PostMapping("/confirm/{id}")
    public String warehouseConfirm(@PathVariable("id") Long id,
                                   @ModelAttribute("receipt") ImportReceipt formReceipt,
                                   RedirectAttributes redirectAttributes) {
        try {
            List<WarehouseCheckDTO> checkResults = new ArrayList<>();

            if (formReceipt.getDetails() != null) {
                for (ImportReceiptDetail detail : formReceipt.getDetails()) {
                    if (detail != null && detail.getId() != null) {
                        WarehouseCheckDTO dto = new WarehouseCheckDTO();
                        dto.setDetailId(detail.getId());
                        dto.setActualQty(detail.getActualQuantity() != null ? detail.getActualQuantity() : 0);
                        dto.setDamagedQty(detail.getDamagedQuantity() != null ? detail.getDamagedQuantity() : 0);

                        checkResults.add(dto);
                    }
                }
            }

            // Thực hiện tính toán khớp số liệu và dồn số lượng vào database tồn kho giày
            importReceiptService.warehouseConfirmAndImportStock(id, checkResults);

            redirectAttributes.addFlashAttribute("successMessage", "Thủ kho đã hoàn tất kiểm kho thành công! Sản phẩm đã dồn vào kho.");
            return "redirect:/warehouse/check/" + id;

        } catch (RuntimeException e) {
            // Bắt các lỗi logic toán học (Ví dụ: Thực nhận + Lỗi != Số lượng duyệt)
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/warehouse/check/" + id;
        }
    }
}
