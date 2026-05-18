package com.example.LVTN.service.impl;

import com.example.LVTN.entity.Category;
import com.example.LVTN.entity.Product;
import com.example.LVTN.repository.ProductRepository;
import com.example.LVTN.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public List<Product> filterProducts(Long categoryId, Double minPrice, Double maxPrice, String keyword) {

        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;


        return productRepository.filterProducts(categoryId, minPrice, maxPrice, searchKeyword);
    }


    @Override
    public List<Product> findFeatured() {
        return productRepository.findByFeaturedTrue();
    }
}