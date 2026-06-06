package com.example.LVTN.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern; // Đảm bảo đã import thư viện này
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

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

    @NotBlank(message = "Tên sản phẩm bắt buộc phải nhập, không được để trống!")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Tên sản phẩm chỉ được chứa chữ cái và khoảng trắng, không được nhập số hoặc ký tự đặc biệt!")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;


    @NotNull(message = "Giá bán không được để trống!")
    @DecimalMin(value = "0.0", message = "Giá bán phải lớn hơn hoặc bằng 0đ!")
    private BigDecimal price;

    private String thumbnail;

    @Column(name = "featured")
    private Boolean featured = false;


    @NotNull(message = "Vui lòng chọn một danh mục sản phẩm!")
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;


    @NotNull(message = "Vui lòng chọn một thương hiệu!")
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