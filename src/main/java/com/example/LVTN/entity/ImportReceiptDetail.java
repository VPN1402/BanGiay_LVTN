package com.example.LVTN.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "import_receipt_details")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportReceiptDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "import_receipt_id")
    private ImportReceipt importReceipt;

    @ManyToOne
    @JoinColumn(name = "product_size_id")
    private ProductSize productSize;

    private Integer quantity;
    private BigDecimal importPrice;


    @Column(name = "subtotal", insertable = false, updatable = false)
    private BigDecimal subtotal;


    @Column(name = "requested_quantity")
    private Integer requestedQuantity; // SL từ file CSV

    @Column(name = "approved_quantity")
    private Integer approvedQuantity;  // SL Admin duyệt mua

    @Column(name = "actual_quantity")
    private Integer actualQuantity;    // SL thực tế kho nhận (Hàng tốt)

    @Column(name = "damaged_quantity")
    private Integer damagedQuantity;   // SL hàng lỗi, rách hộp

    @Column(name = "is_approved")
    private Boolean isApproved;        // Admin có chọn mua mã này không
}