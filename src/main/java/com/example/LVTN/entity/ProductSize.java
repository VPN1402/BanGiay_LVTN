package com.example.LVTN.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_sizes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String size;

    private Integer quantity;

    @Column(name = "min_quantity")
    private Integer minQuantity;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
