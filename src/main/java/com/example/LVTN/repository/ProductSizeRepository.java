package com.example.LVTN.repository;

import com.example.LVTN.entity.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {

    List<ProductSize> findByProductId(Long productId);
    // Thêm hàm này để chốt tồn kho theo product_id và size
    Optional<ProductSize> findByProductIdAndSize(Long productId, String size);
}