package com.example.LVTN.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "procurement_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProcurementRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status = "OPEN"; // OPEN, CLOSED

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Liên kết với danh sách các báo giá (ImportReceipts) nộp vào đợt này
    @OneToMany(mappedBy = "procurementRequest", cascade = CascadeType.ALL)
    private List<ImportReceipt> bids = new ArrayList<>();
}