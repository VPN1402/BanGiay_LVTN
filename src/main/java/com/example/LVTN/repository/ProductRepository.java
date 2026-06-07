package com.example.LVTN.repository;

import com.example.LVTN.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByFeaturedTrue();

    // CÂU LỆNH JPQL NÂNG CẤP LỌC THEO CATEGORY, BRAND, PRICE VÀ SIZE
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
}