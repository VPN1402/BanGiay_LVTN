package com.example.LVTN.repository;

import com.example.LVTN.dto.ProductSaleDTO;
import com.example.LVTN.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByFeaturedTrue();

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN p.category c " +
            "LEFT JOIN p.brand b " +
            "LEFT JOIN p.productSizes ps " + // Join sang bảng size để lọc size
            "WHERE (:categoryId IS NULL OR c.id = :categoryId) " +
            "AND (:brandId IS NULL OR b.id = :brandId) " + // Thêm lọc Brand
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:sizeName IS NULL OR ps.size = :sizeName) " + // Thêm lọc Size (ví dụ: "40", "41")
            "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> filterProducts(@Param("categoryId") Long categoryId,
                                 @Param("brandId") Long brandId, // Thêm tham số
                                 @Param("minPrice") Double minPrice,
                                 @Param("maxPrice") Double maxPrice,
                                 @Param("sizeName") String sizeName, // Thêm tham số
                                 @Param("keyword") String keyword,
                                 Pageable pageable);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByName(@Param("keyword") String keyword);

    // 1. Lấy Top Sản phẩm Bán Chạy (Chỉ tính các đơn hàng đã giao - DELIVERED)
    @Query("SELECT new com.example.LVTN.dto.ProductSaleDTO(" +
            "p.id, p.name, " +
            "COALESCE(SUM(oi.quantity), 0L), " +
            "(SELECT COALESCE(SUM(ps.quantity), 0L) FROM ProductSize ps WHERE ps.product.id = p.id)) " +
            "FROM Product p " +
            "LEFT JOIN OrderItem oi ON p.id = oi.product.id " +
            "LEFT JOIN oi.order o ON o.id = oi.order.id AND o.orderStatus = 'DELIVERED' " +
            "GROUP BY p.id, p.name " +
            "ORDER BY COALESCE(SUM(oi.quantity), 0L) DESC")
    Page<ProductSaleDTO> getTopSellingProducts(Pageable pageable);

    // 2. Lấy Top Sản phẩm Bán Chậm (Ít người mua nhất)
    @Query("SELECT new com.example.LVTN.dto.ProductSaleDTO(" +
            "p.id, p.name, " +
            "COALESCE(SUM(oi.quantity), 0L), " +
            "(SELECT COALESCE(SUM(ps.quantity), 0L) FROM ProductSize ps WHERE ps.product.id = p.id)) " +
            "FROM Product p " +
            "LEFT JOIN OrderItem oi ON p.id = oi.product.id " +
            "LEFT JOIN oi.order o ON o.id = oi.order.id AND o.orderStatus = 'DELIVERED' " +
            "GROUP BY p.id, p.name " +
            "ORDER BY COALESCE(SUM(oi.quantity), 0L) ASC")
    Page<ProductSaleDTO> getSlowSellingProducts(Pageable pageable);

    // 3. Đếm số lượng sản phẩm chưa bán được cái nào (Để hiện lên chỉ số "Sản phẩm bán chậm")
    @Query("SELECT COUNT(p) FROM Product p WHERE p.id NOT IN " +
            "(SELECT oi.product.id FROM OrderItem oi JOIN oi.order o WHERE o.orderStatus = 'DELIVERED')")
    Long countProductsWithZeroSales();



    // 5. Tính giá trị vốn đọng (Tổng giá trị hàng hóa hiện có trong kho: Số lượng tồn * Giá bán)
    @Query(value = "SELECT SUM(ps.quantity * p.price) FROM product_sizes ps " +
            "JOIN products p ON ps.product_id = p.id", nativeQuery = true)
    java.math.BigDecimal calculateDeadCapital();

    // 6. Thống kê tỷ lệ phần trăm doanh thu theo từng thương hiệu (Phục vụ biểu đồ thanh ngang)
    @Query(value = "SELECT b.name as brandName, " +
            "       ROUND((SUM(oi.quantity * oi.price) / (SELECT SUM(total_amount) FROM orders WHERE order_status = 'DELIVERED')) * 100, 1) as percentage " +
            "FROM brands b " +
            "JOIN products p ON p.brand_id = b.id " +
            "JOIN order_items oi ON oi.product_id = p.id " +
            "JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.order_status = 'DELIVERED' " +
            "GROUP BY b.id, b.name " +
            "ORDER BY percentage DESC", nativeQuery = true)
    List<Object[]> getRevenuePercentageByBrand();
}