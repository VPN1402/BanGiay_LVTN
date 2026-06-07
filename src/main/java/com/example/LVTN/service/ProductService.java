package com.example.LVTN.service;

import com.example.LVTN.entity.Category;
import com.example.LVTN.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    List<Product> findAll();

    Product findById(Long id);

    Product save(Product product);

    void delete(Long id);

    Page<Product> filterProducts(Long categoryId, Long brandId, Double minPrice, Double maxPrice, String sizeName, String keyword, Pageable pageable);

    List<Product> findFeatured();

    Page<Product> findAll(Pageable pageable);
}