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
}