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

    private String status = "OPEN";

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();


    @OneToMany(mappedBy = "procurementRequest", cascade = CascadeType.ALL)
    private List<ImportReceipt> bids = new ArrayList<>();

    @OneToMany(mappedBy = "procurementRequest", cascade = CascadeType.ALL)
    private List<ProcurementRequestDetail> details = new ArrayList<>();
}