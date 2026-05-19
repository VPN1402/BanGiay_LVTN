package com.example.LVTN.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "import_receipts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    private LocalDateTime importDate = LocalDateTime.now();
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "importReceipt", cascade = CascadeType.ALL)
    private List<ImportReceiptDetail> details = new ArrayList<>();
}