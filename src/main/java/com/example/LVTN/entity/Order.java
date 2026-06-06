package com.example.LVTN.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private BigDecimal totalAmount;
    private String orderStatus;    // Sẽ lưu OrderStatus.name()
    private String paymentStatus;  // Sẽ lưu PaymentStatus.name()
    private String paymentMethod;  // "COD" hoặc "VNPAY"

    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> orderItems = new ArrayList<>();
    // Thêm vào bên trong class Order ở file Order.java của bạn

    private java.math.BigDecimal finalTotal;  // Dùng để lấy số tiền thanh toán (getFinalTotal)


}