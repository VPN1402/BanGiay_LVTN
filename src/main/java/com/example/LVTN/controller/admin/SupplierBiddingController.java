package com.example.LVTN.controller.admin;

import com.example.LVTN.entity.*;
import com.example.LVTN.repository.ProcurementRequestDetailRepository;
import com.example.LVTN.repository.ProcurementRequestRepository;
import com.example.LVTN.repository.ProductSizeRepository;
import com.example.LVTN.repository.SupplierRepository;
import com.example.LVTN.service.ImportReceiptService;
import com.example.LVTN.service.SupplierService;
import com.example.LVTN.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

    @Autowired
    private ProcurementRequestRepository procRequestRepo;


    @GetMapping("/bid/create")
    public String showImportStockForm(Model model, Principal principal) {
        List<Supplier> allSuppliers = supplierRepository.findAll();
        model.addAttribute("allSuppliers", allSuppliers);
        if (principal != null) {
            String loggedInEmail = principal.getName();


            User currentUser = userService.findByEmail(loggedInEmail);

            if (currentUser != null) {

                model.addAttribute("supplierName", currentUser.getFullName());


                if (currentUser.getSupplier() != null) {
                    model.addAttribute("currentSupplierId", currentUser.getSupplier().getId());
                }
            }
        }


        return "admin/supplier/import-stock";
    }

    // 2. Xử lý đọc file CSV báo giá từ Nhà Cung Cấp
    @PostMapping("/bid/save")
    public String saveSupplierBid(@RequestParam("file") MultipartFile file,
                                  @RequestParam("requestId") Long requestId, // ID của đợt thu mua từ form
                                  @RequestParam(value = "note", required = false) String note,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn một file CSV hợp lệ!");
                return "redirect:/supplier/bid/create/" + requestId;
            }


            User loggedInUser = userService.findByEmail(principal.getName());
            if (loggedInUser == null || loggedInUser.getSupplier() == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản chưa được liên kết với Nhà Cung Cấp!");
                return "redirect:/supplier/bid/list";
            }
            Supplier currentSupplier = loggedInUser.getSupplier();

            // 2. Lấy đợt thu mua từ database
            ProcurementRequest request = procRequestRepo.findById(requestId).orElse(null);
            if (request == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Đợt thu mua không tồn tại!");
                return "redirect:/supplier/bid/list";
            }

            //khoi tao phieu bao gia
            ImportReceipt receipt = new ImportReceipt();
            receipt.setSupplier(currentSupplier);
            receipt.setUser(loggedInUser);
            receipt.setNote(note);
            receipt.setStatus("PENDING");


            receipt.setProcurementRequest(request);

            List<ImportReceiptDetail> detailsList = new ArrayList<>();

            //  Đọc file CSV
            try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
                String line;
                boolean isHeader = true;

                while ((line = br.readLine()) != null) {
                    if (isHeader) { isHeader = false; continue; }
                    if (line.trim().isEmpty()) continue;

                    String[] data = line.split(",|;");
                    if (data.length < 5) continue;

                    try {
                        Long sizeId = Long.parseLong(data[2].trim());
                        Integer reqQty = Integer.parseInt(data[3].trim());
                        BigDecimal price = new BigDecimal(data[4].trim());

                        ProductSize ps = productSizeRepository.findById(sizeId).orElse(null);
                        if (ps != null) {
                            ImportReceiptDetail detail = new ImportReceiptDetail();
                            detail.setImportReceipt(receipt);
                            detail.setProductSize(ps);
                            detail.setQuantity(reqQty);
                            detail.setRequestedQuantity(reqQty);
                            detail.setImportPrice(price);
                            detail.setApprovedQuantity(0);
                            detail.setActualQuantity(0);
                            detail.setDamagedQuantity(0);
                            detail.setIsApproved(false);

                            detailsList.add(detail);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Dòng lỗi định dạng: " + line);
                        continue;
                    }
                }
            }

            if (detailsList.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy dữ liệu hợp lệ trong file!");
                return "redirect:/supplier/bid/create/" + requestId;
            }

            receipt.setDetails(detailsList);


            importReceiptService.saveDraftReceipt(receipt);

            redirectAttributes.addFlashAttribute("successMessage", "Nộp báo giá cho đợt #" + requestId + " thành công!");
            return "redirect:/supplier/bid/list";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/supplier/bid/create/" + requestId;
        }
    }
    @GetMapping("/bid/list")
    public String listBiddableRequests(Model model) {

        model.addAttribute("requests", procRequestRepo.findByStatus("SENT"));
        return "admin/supplier/bid-list";
    }

    // Khi nhấn "Nộp báo giá" từ trang danh sách, truyền ID qua
    @GetMapping("/bid/create/{requestId}")
    public String showImportStockForm(@PathVariable("requestId") Long requestId, Model model) {

        ProcurementRequest request = procRequestRepo.findById(requestId).orElse(null);
        model.addAttribute("request", request);
        model.addAttribute("requestId", requestId);


        model.addAttribute("allSuppliers", supplierRepository.findAll());

        return "admin/supplier/import-stock";
    }
    //trang chi tiet
    @GetMapping("/bid/detail/{id}")
    public String viewBidDetail(@PathVariable("id") Long id, Model model) {
        ProcurementRequest request = procRequestRepo.findById(id).orElse(null);
        if (request == null) {
            return "redirect:/supplier/bid/list";
        }
        model.addAttribute("request", request);
        return "admin/supplier/bid-detail";
    }

    @GetMapping("/bid/export-csv/{id}")
    public void exportCsvTemplate(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        ProcurementRequest request = procRequestRepo.findById(id).orElse(null);
        if (request == null) return;

        // Thiết lập Header để trình duyệt tự hiểu đây là file tải về
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=YeuCauBaoGia_Dot_" + id + ".csv");

        PrintWriter writer = response.getWriter();

        // Ghi mã BOM để Excel không bị lỗi font tiếng Việt khi mở
        writer.write('\uFEFF');

        // Ghi dòng tiêu đề cột (Đúng chuẩn index mà hàm đọc file CSV của bạn đang yêu cầu)
        writer.println("Mã sản phẩm,Tên sản phẩm,Mã kích thước,Số lượng yêu cầu,Giá nhập thầu");

        // Duyệt qua danh sách Admin yêu cầu để ghi vào file CSV
        for (ProcurementRequestDetail detail : request.getDetails()) {
            Long productId = detail.getProductSize().getProduct().getId();
            String productName = detail.getProductSize().getProduct().getName().replace(",", " "); // Tránh lỗi dấu phẩy trong CSV
            Long sizeId = detail.getProductSize().getId();
            Integer quantity = detail.getQuantityNeeded(); // Lấy từ trường quantityNeeded trong Entity của bạn

            // Dòng dữ liệu mẫu (để trống cột cuối cùng "Giá nhập thầu" hoặc để là 0 để NCC tự điền)
            writer.println(productId + "," + productName + "," + sizeId + "," + quantity + ",0");
        }

        writer.flush();
        writer.close();
    }
}