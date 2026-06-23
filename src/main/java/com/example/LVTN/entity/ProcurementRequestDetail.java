package com.example.LVTN.entity;

import com.example.LVTN.entity.ProcurementRequest;
import com.example.LVTN.entity.ProductSize;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "procurement_request_details")
@Getter
@Setter
public class ProcurementRequestDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "procurement_request_id")
    private ProcurementRequest procurementRequest;

    @ManyToOne
    @JoinColumn(name = "product_size_id")
    private ProductSize productSize;

    private Integer quantityNeeded; // Số lượng hệ thống cần bù vào
}