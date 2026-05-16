package com.example.LVTN.service;

import com.example.LVTN.entity.Category;
import com.example.LVTN.entity.Product;

import java.util.List;

public interface ProductService {

    List<Product> findAll();

    Product findById(Long id);

    Product save(Product product);

    void delete(Long id);

    List<Product> filterProducts(Long categoryId, Double minPrice, Double maxPrice);
    List<Product> findFeatured();
}