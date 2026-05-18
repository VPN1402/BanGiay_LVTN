package com.example.LVTN.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private int size;
    private int quantity;
    private BigDecimal price;

    // Hàm tiện ích tính tổng tiền của riêng item này (Trả về kiểu BigDecimal)
    public BigDecimal getAmount() {
        if (this.price == null) return BigDecimal.ZERO;
        return this.price.multiply(BigDecimal.valueOf(this.quantity));
    }
}