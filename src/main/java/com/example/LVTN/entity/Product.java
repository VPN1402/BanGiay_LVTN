package com.example.LVTN.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List; // Thêm thư viện List

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal price;

    private String thumbnail;

    @Column(name = "featured")
    private Boolean featured = false;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand;



    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductSize> productSizes;


    public int getTotalQuantity() {
        if (productSizes == null || productSizes.isEmpty()) {
            return 0;
        }
        return productSizes.stream()
                .mapToInt(ps -> ps.getQuantity() != null ? ps.getQuantity() : 0)
                .sum();
    }
}