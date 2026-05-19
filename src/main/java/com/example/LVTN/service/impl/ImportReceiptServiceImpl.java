package com.example.LVTN.service.impl;

import com.example.LVTN.entity.ImportReceipt;
import com.example.LVTN.entity.ImportReceiptDetail;
import com.example.LVTN.entity.ProductSize;
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
    private ProductSizeRepository productSizeRepository;

    @Override
    public void saveReceipt(ImportReceipt receipt) {
        // 1. Tính tổng tiền từ các detail (nếu tầng DB hoặc code chưa tự tính)
        BigDecimal total = BigDecimal.ZERO;

        // 2. Lưu và cộng dồn tồn kho
        for (ImportReceiptDetail detail : receipt.getDetails()) {
            // Lấy ProductSize từ DB lên để tránh mất dữ liệu liên kết quan hệ
            ProductSize ps = productSizeRepository.findById(detail.getProductSize().getId()).orElse(null);
            if (ps != null) {
                // Cộng thêm số lượng nhập kho vào kho hiện tại
                ps.setQuantity(ps.getQuantity() + detail.getQuantity());
                productSizeRepository.save(ps);

                // Gán ngược đối tượng đã đầy đủ thông tin vào detail để Thymeleaf hiển thị được luôn
                detail.setProductSize(ps);
            }

            // Tính toán tổng tiền phiếu nhập
            BigDecimal subtotal = detail.getImportPrice().multiply(new BigDecimal(detail.getQuantity()));
            total = total.add(subtotal);
        }

        receipt.setTotalAmount(total);
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