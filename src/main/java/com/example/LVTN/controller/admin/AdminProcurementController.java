package com.example.LVTN.controller.admin;

import com.example.LVTN.dto.ImportDetailUpdateDTO;
import com.example.LVTN.dto.ImportUpdateForm;
import com.example.LVTN.dto.WarehouseCheckDTO;
import com.example.LVTN.entity.*;
import com.example.LVTN.repository.*;
import com.example.LVTN.service.ImportReceiptService;
import com.example.LVTN.service.ProcurementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminProcurementController {

    @Autowired
    private ImportReceiptService importReceiptService;
    @Autowired
    private ImportReceiptDetailRepository importReceiptDetailRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductSizeRepository productSizeRepository;
    @Autowired
    private ProcurementService procurementService;
    @Autowired
    private ProcurementRequestRepository procRequestRepo;
    @Autowired
    private ProcurementRequestDetailRepository procDetailRepo;


    @GetMapping("/import/list")
    public String listAllReceipts(Model model) {
        model.addAttribute("receipts", importReceiptService.findAll());
        return "admin/import/import-list";
    }

    @GetMapping("/import/detail/{id}")
    public String showImportDetail(@PathVariable("id") Long id, Model model) {
        ImportReceipt receipt = importReceiptService.findById(id);
        model.addAttribute("receipt", receipt);
        model.addAttribute("updateForm", new ImportUpdateForm());
        return "admin/import/import-detail";
    }

    // HÀM PHÊ DUYỆT CỦA ADMIN: Lưu chi tiết + Chuyển trạng thái sang APPROVED
    @PostMapping("/import/approve/{id}")
    public String approveImportReceipt(@PathVariable("id") Long id,
                                       @ModelAttribute("updateForm") ImportUpdateForm updateForm,
                                       Principal principal,
                                       RedirectAttributes redirectAttributes) {
        try {
            List<ImportDetailUpdateDTO> details = updateForm.getAdminDetails();

            if (details == null || details.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không có dữ liệu chi tiết!");
                return "redirect:/admin/import/detail/" + id;
            }

            boolean hasSelected = false;
            // Duyệt qua danh sách để kiểm tra
            for (ImportDetailUpdateDTO dto : details) {
                // CHỈ XỬ LÝ NẾU CÓ TÍCH CHỌN
                if (Boolean.TRUE.equals(dto.getIsSelected())) {
                    // Kiểm tra xem Admin có nhập số lượng hợp lệ không
                    if (dto.getApprovedQty() == null || dto.getApprovedQty() <= 0) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                "Sản phẩm đã chọn phải có số lượng lớn hơn 0!");
                        return "redirect:/admin/import/detail/" + id;
                    }
                    hasSelected = true;
                }
            }

            if (!hasSelected) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa tích chọn sản phẩm nào để phê duyệt!");
                return "redirect:/admin/import/detail/" + id;
            }

            // 1. Lưu các chi tiết được duyệt vào DB (Sử dụng hàm của service xử lý logic bên trên)
            importReceiptService.adminApproveReceipt(id, details);

            // 2. Cập nhật trạng thái phiếu
            ImportReceipt receipt = importReceiptService.findById(id);
            if (principal != null) {
                User currentAdmin = userRepository.findByEmail(principal.getName()).orElse(null);
                receipt.setApprovedBy(currentAdmin);
            }
            receipt.setStatus("APPROVED");
            importReceiptService.save(receipt);

            redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt thành công! Phiếu đã chuyển bộ phận kho.");
            return "redirect:/admin/import/detail/" + id;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/import/detail/" + id;
        }
    }

    // HÀM XỬ LÝ THỦ KHO HOÀN TẤT KIỂM NHẬN
    @PostMapping("/import/warehouse/confirm/{id}")
    public String confirmWarehouseCheck(@PathVariable("id") Long id,
                                        @ModelAttribute("updateForm") ImportUpdateForm updateForm,
                                        Principal principal,
                                        RedirectAttributes redirectAttributes) {
        try {
            ImportReceipt receipt = importReceiptService.findById(id);

            if ("COMPLETED".equals(receipt.getStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Phiếu nhập này đã hoàn tất rồi!");
                return "redirect:/admin/import/detail/" + id;
            }

            if (principal != null) {
                receipt.setCompletedBy(userRepository.findByEmail(principal.getName()).orElse(null));
            }

            List<WarehouseCheckDTO> list = updateForm.getWarehouseDetails();

            if (list != null) {
                for (WarehouseCheckDTO dto : list) {
                    if (dto == null || dto.getDetailId() == null) continue;

                    ImportReceiptDetail d = importReceiptDetailRepository.findById(dto.getDetailId()).orElse(null);
                    if (d == null) continue;

                    // --- SỬA LỖI TẠI ĐÂY: Xử lý an toàn với null ---
                    // Nếu ApprovedQuantity là null thì coi như là 0
                    int approvedQty = (d.getApprovedQuantity() == null) ? 0 : d.getApprovedQuantity();

                    int actual = (dto.getActualQty() == null) ? 0 : dto.getActualQty();
                    int damaged = (dto.getDamagedQty() == null) ? 0 : dto.getDamagedQty();

                    int totalInput = actual + damaged;

                    if (totalInput != approvedQty) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                "Lỗi số lượng sản phẩm ID " + d.getId() + ": Hàng tốt (" + actual + ") + Hàng lỗi (" + damaged + ") phải bằng số lượng đã duyệt (" + approvedQty + ")");
                        return "redirect:/admin/import/detail/" + id;
                    }

                    // Lưu thông tin kiểm kho
                    d.setActualQuantity(actual);
                    d.setDamagedQuantity(damaged);
                    importReceiptDetailRepository.save(d);

                    // Cập nhật tồn kho (ProductSize)
                    ProductSize ps = d.getProductSize();
                    if (ps != null) {
                        int oldQty = (ps.getQuantity() == null) ? 0 : ps.getQuantity();
                        ps.setQuantity(oldQty + actual);
                        productSizeRepository.save(ps);
                    }
                }
            }

            receipt.setStatus("COMPLETED");
            importReceiptService.save(receipt);

            redirectAttributes.addFlashAttribute("successMessage", "Hoàn tất kiểm kho thành công!");
            return "redirect:/admin/import/detail/" + id;

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/admin/import/detail/" + id;
        }
    }

    // Trang danh sách các đợt thu mua
    @GetMapping("procurement/list")
    public String listRequests(Model model) {
        model.addAttribute("requests", procRequestRepo.findAll());
        return "admin/procurement/list";
    }

    // Nút bấm để quét tồn kho và tạo đợt mới
    @PostMapping("/procurement/generate")
    public String generateRequest(@RequestParam(value = "note", required = false) String note,
                                  RedirectAttributes redirectAttributes) {
        try {
            // Truyền tham số note vào hàm xử lý của Service
            procurementService.generateRequestIfNeeded(note);

            redirectAttributes.addFlashAttribute("successMessage", "Đã quét tồn kho hệ thống và lập đợt thu mua mới thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi phát sinh khi lập lịch thu mua: " + e.getMessage());
        }
        return "redirect:/admin/procurement/list";
    }
    @GetMapping("procurement/detail/{id}")
    public String detailRequest(@PathVariable("id") Long id, Model model) {
        ProcurementRequest request = procurementService.getDetailById(id);
        model.addAttribute("request", request);
        return "admin/procurement/detail";
    }
    @PostMapping("procurement/update-details")
    public String updateDetails(@RequestParam("detailIds") List<Long> ids,
                                @RequestParam("quantities") List<Integer> quantities) {
        for (int i = 0; i < ids.size(); i++) {
            ProcurementRequestDetail detail = procDetailRepo.findById(ids.get(i)).orElse(null);
            if (detail != null) {
                detail.setQuantityNeeded(quantities.get(i)); // Cập nhật số lượng mới
                procDetailRepo.save(detail);
            }
        }
        return "redirect:/admin/procurement/list";
    }


    @PostMapping("/procurement/send/{id}")
    public String sendToSuppliers(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        ProcurementRequest request = procurementService.getDetailById(id);
        if (request != null) {
            request.setStatus("SENT"); // Chuyển trạng thái
            procRequestRepo.save(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi thông báo cho nhà cung cấp thành công!");
        }
        return "redirect:/admin/procurement/list";
    }
}