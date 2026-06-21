package com.example.LVTN.controller.admin;

import com.example.LVTN.entity.*;
import com.example.LVTN.repository.ProductSizeRepository;
import com.example.LVTN.repository.SupplierRepository;
import com.example.LVTN.service.ImportReceiptService;
import com.example.LVTN.service.SupplierService;
import com.example.LVTN.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/supplier")
public class SupplierBiddingController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductSizeRepository productSizeRepository;

    @Autowired
    private ImportReceiptService importReceiptService;

    @Autowired
    private SupplierRepository supplierRepository;



    @GetMapping("/bid/create") // Hoặc đường dẫn GET hiển thị form này của bạn
    public String showImportStockForm(Model model, Principal principal) {
        List<Supplier> allSuppliers = supplierRepository.findAll(); // hoặc supplierRepository.findAll()
        model.addAttribute("allSuppliers", allSuppliers);
        if (principal != null) {
            String loggedInEmail = principal.getName(); // Lấy email tài khoản đang đăng nhập

            // Dùng userService (hoặc userRepository) để tìm thực thể User đầy đủ thông tin trong DB
            User currentUser = userService.findByEmail(loggedInEmail);

            if (currentUser != null) {
                // Đẩy tên đầy đủ (fullName) và thông tin Nhà cung cấp sang giao diện
                model.addAttribute("supplierName", currentUser.getFullName());

                // Nếu bạn cần bốc thêm ID nhà cung cấp để lát lưu dữ liệu:
                if (currentUser.getSupplier() != null) {
                    model.addAttribute("currentSupplierId", currentUser.getSupplier().getId());
                }
            }
        }

        // Thêm các dữ liệu khác nếu có (ví dụ: danh sách sản phẩm...)
        return "admin/supplier/import-stock"; // Trỏ đúng về file HTML đang bị lỗi của bạn
    }

    // 2. Xử lý đọc file CSV báo giá từ Nhà Cung Cấp
    @PostMapping("/bid/save")
    public String saveSupplierBid(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "note", required = false) String note, // Thêm required = false để tránh lỗi nếu note trống
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn một file CSV hợp lệ!");
                return "redirect:/supplier/bid/create";
            }

            // BẢO MẬT: Lấy tự động thông tin nhà cung cấp đang đăng nhập thông qua phiên (Principal)
            User loggedInUser = userService.findByEmail(principal.getName());
            if (loggedInUser == null || loggedInUser.getSupplier() == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản của bạn chưa được liên kết với Nhà Cung Cấp nào!");
                return "redirect:/supplier/bid/create";
            }
            Supplier currentSupplier = loggedInUser.getSupplier();

            // Khởi tạo Phiếu báo giá dạng PENDING
            ImportReceipt receipt = new ImportReceipt();
            receipt.setSupplier(currentSupplier);
            receipt.setUser(loggedInUser);
            receipt.setNote(note);
            receipt.setStatus("PENDING");

            List<ImportReceiptDetail> detailsList = new ArrayList<>();

            // Đọc file CSV dữ liệu báo giá
            try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
                String line;
                boolean isHeader = true;

                while ((line = br.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false; // Bỏ qua dòng tiêu đề đầu tiên của file CSV
                        continue;
                    }

                    // Bỏ qua nếu gặp dòng trống trong file
                    if (line.trim().isEmpty()) continue;

                    // Tách chuỗi hỗ trợ cả dấu phẩy (,) và dấu chấm phẩy (;) đề phòng định dạng Excel thay đổi
                    String[] data = line.split(",|;");
                    if (data.length < 5) continue; // File của bạn bắt buộc phải có từ 5 cột trở lên

                    try {
                        // ĐỌC ĐÚNG CHỈ SỐ INDEX THEO FILE CSV CỦA BẠN:
                        // data[0] = product_id (Bỏ qua, không cần parse nếu không dùng đến)
                        // data[1] = product_name (Đây là chuỗi "Giay Nike Air Force 1", không được parse sang số)

                        Long sizeId = Long.parseLong(data[2].trim());          // Cột số 3 (Index 2): size_id
                        Integer reqQty = Integer.parseInt(data[3].trim());       // Cột số 4 (Index 3): requested_quantity
                        BigDecimal price = new BigDecimal(data[4].trim());     // Cột số 5 (Index 4): bid_price

                        ProductSize ps = productSizeRepository.findById(sizeId).orElse(null);
                        if (ps != null) {
                            ImportReceiptDetail detail = new ImportReceiptDetail();
                            detail.setImportReceipt(receipt);
                            detail.setProductSize(ps);

                            // GÁN CẢ 2 ĐỂ TRÁNH LỖI (Tùy thuộc vào Entity của bạn đang dùng trường nào làm số lượng gốc)
                            detail.setRequestedQuantity(reqQty);

                            // BỔ SUNG DÒNG NÀY: Giúp hàm getQuantity() không bao giờ bị null nữa
                            detail.setQuantity(reqQty);

                            detail.setImportPrice(price);

                            // Mặc định thiết lập ban đầu bằng 0
                            detail.setApprovedQuantity(0);
                            detail.setActualQuantity(0);
                            detail.setDamagedQuantity(0);
                            detail.setIsApproved(false);

                            detailsList.add(detail);
                        }
                    } catch (NumberFormatException e) {
                        // Nếu có dòng nào bị lỗi định dạng số, bỏ qua dòng đó và tiếp tục đọc, tránh sập toàn hệ thống
                        System.err.println("Bỏ qua dòng lỗi định dạng dữ liệu số: " + line);
                        continue;
                    }
                }
            }

            if (detailsList.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy dữ liệu báo giá hợp lệ nào trong file CSV!");
                return "redirect:/supplier/bid/create";
            }

            receipt.setDetails(detailsList);

            // Gọi dịch vụ lưu nháp phiếu đấu thầu ẩn
            importReceiptService.saveDraftReceipt(receipt);

            redirectAttributes.addFlashAttribute("successMessage", "Nộp hồ sơ báo giá thành công! Vui lòng chờ kết quả đấu thầu.");
            return "redirect:/supplier/bid/create";

        } catch (Exception e) {
            e.printStackTrace(); // In chi tiết log lỗi ra console phục vụ debug
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xử lý file CSV: " + e.getMessage());
            return "redirect:/supplier/bid/create";
        }
    }
}