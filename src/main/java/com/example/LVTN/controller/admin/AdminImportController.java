package com.example.LVTN.controller.admin;

import com.example.LVTN.dto.ImportDetailUpdateDTO;
import com.example.LVTN.dto.WarehouseCheckDTO;
import com.example.LVTN.entity.ImportReceipt;
import com.example.LVTN.entity.ImportReceiptDetail;
import com.example.LVTN.entity.ProductSize;
import com.example.LVTN.entity.Supplier;
import com.example.LVTN.entity.User;
import com.example.LVTN.repository.ProductSizeRepository;
import com.example.LVTN.repository.SupplierRepository;
import com.example.LVTN.service.ImportReceiptService;
import com.example.LVTN.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminImportController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductSizeRepository productSizeRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ImportReceiptService importReceiptService;

    // 1. Hiển thị danh sách lịch sử/tiến độ các phiếu nhập kho
    @GetMapping("/import/list")
    public String listImports(Model model) {
        model.addAttribute("receipts", importReceiptService.findAll());
        return "admin/import/import-list";
    }

    // 2. Hiển thị form tạo phiếu yêu cầu nhập (Giao diện upload file CSV)
    @GetMapping("/import/create")
    public String showCreateForm(Model model) {
        model.addAttribute("allSuppliers", supplierRepository.findAll());
        return "admin/import/import-stock";
    }

    // BƯỚC 1: XỬ LÝ ĐỌC FILE CSV VÀ LƯU NHÁP PHIẾU NHẬP YÊU CẦU
    @PostMapping("/import/save")
    public String saveImport(@RequestParam("supplierId") Long supplierId,
                             @RequestParam("note") String note,
                             @RequestParam("file") MultipartFile file,
                             Principal principal,
                             Model model) {
        try {
            // Kiểm tra file trống
            if (file.isEmpty()) {
                model.addAttribute("error", "Vui lòng chọn một file CSV để upload!");
                model.addAttribute("allSuppliers", supplierRepository.findAll());
                return "admin/import/import-stock";
            }

            // Khởi tạo đối tượng phiếu nhập kho
            ImportReceipt receipt = new ImportReceipt();
            receipt.setNote(note);
            receipt.setStatus("PENDING_APPROVAL"); // Mặc định bước đầu tiên là chờ duyệt

            // Thiết lập Người nhập đơn
            User user = userService.findByEmail(principal.getName());
            receipt.setUser(user);

            // Thiết lập Nhà cung cấp
            Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
            if (supplier == null) {
                model.addAttribute("error", "Nhà cung cấp không tồn tại!");
                model.addAttribute("allSuppliers", supplierRepository.findAll());
                return "admin/import/import-stock";
            }
            receipt.setSupplier(supplier);

            // Xử lý đọc file CSV
            List<ImportReceiptDetail> details = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
                String line;
                boolean isHeader = true;

                while ((line = br.readLine()) != null) {
                    // Bỏ qua dòng tiêu đề nếu có
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }

                    // Cắt dữ liệu theo dấu phẩy (CSV tiêu chuẩn)
                    String[] data = line.split(",");
                    if (data.length >= 3) {
                        Long productSizeId = Long.parseLong(data[0].trim());
                        Integer quantity = Integer.parseInt(data[1].trim());
                        BigDecimal importPrice = new BigDecimal(data[2].trim());

                        // Tìm ProductSize tương ứng trong DB
                        ProductSize ps = productSizeRepository.findById(productSizeId).orElse(null);
                        if (ps != null) {
                            ImportReceiptDetail detail = new ImportReceiptDetail();
                            detail.setProductSize(ps);
                            detail.setQuantity(quantity);
                            detail.setRequestedQuantity(quantity); // Gán số lượng gốc từ CSV
                            detail.setImportPrice(importPrice);
                            detail.setImportReceipt(receipt);

                            details.add(detail);
                        }
                    }
                }
            }

            // Gán danh sách chi tiết vào phiếu và lưu nháp
            receipt.setDetails(details);
            importReceiptService.saveDraftReceipt(receipt);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Đã xảy ra lỗi khi đọc file CSV: " + e.getMessage());
            model.addAttribute("allSuppliers", supplierRepository.findAll());
            return "admin/import/import-stock";
        }

        // Đã sửa lại đường dẫn redirect chính xác
        return "redirect:/admin/import/list";
    }

    // Xem chi tiết phiếu nhập (Dùng chung cho cả lúc xem, lúc duyệt và lúc kiểm kho)
    @GetMapping("/import/detail/{id}")
    public String viewDetail(@PathVariable("id") Long id, Model model) {
        ImportReceipt receipt = importReceiptService.findById(id);
        model.addAttribute("receipt", receipt);
        return "admin/import/import-detail";
    }

    // BƯỚC 2: Xử lý khi Admin bấm nút phê duyệt phiếu nhập (Chốt số lượng mua)
    @PostMapping("/import/approve/{id}")
    public String adminApprove(@PathVariable("id") Long id,
                               @RequestParam("detailId") List<Long> detailIds,
                               @RequestParam(value = "isSelected", required = false) List<Long> selectedIds,
                               @RequestParam("approvedQty") List<Integer> approvedQties) {

        // Chuyển đổi dữ liệu từ Request về list DTO
        List<ImportDetailUpdateDTO> decisions = new ArrayList<>();
        for (int i = 0; i < detailIds.size(); i++) {
            ImportDetailUpdateDTO dto = new ImportDetailUpdateDTO();
            dto.setDetailId(detailIds.get(i));
            dto.setApprovedQty(approvedQties.get(i));

            // Kiểm tra xem sản phẩm này có được check chọn duyệt hay không
            Long currentId = detailIds.get(i);
            boolean isChecked = (selectedIds != null && selectedIds.contains(currentId));
            dto.setIsSelected(isChecked);

            decisions.add(dto);
        }

        importReceiptService.adminApproveReceipt(id, decisions);
        return "redirect:/admin/import/detail/" + id;
    }

    // BƯỚC 3: Xử lý khi Cửa hàng trưởng kiểm đếm thực tế tại kho và CHỐT CỘNG TỒN KHO
    @PostMapping("/import/confirm-warehouse/{id}")
    public String warehouseConfirm(@PathVariable("id") Long id,
                                   @RequestParam("detailId") List<Long> detailIds,
                                   @RequestParam("actualQty") List<Integer> actualQties,
                                   @RequestParam("damagedQty") List<Integer> damagedQties) {

        // Chuyển đổi dữ liệu từ form kiểm đếm về list DTO
        List<WarehouseCheckDTO> checkResults = new ArrayList<>();
        for (int i = 0; i < detailIds.size(); i++) {
            WarehouseCheckDTO dto = new WarehouseCheckDTO();
            dto.setDetailId(detailIds.get(i));
            dto.setActualQty(actualQties.get(i));
            dto.setDamagedQty(damagedQties.get(i));

            checkResults.add(dto);
        }

        importReceiptService.warehouseConfirmAndImportStock(id, checkResults);
        return "redirect:/admin/import/detail/" + id;
    }
}