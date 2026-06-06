package com.example.LVTN.service;

import com.example.LVTN.entity.Category;

import java.util.List;

public interface CategoryService {

    List<Category> findAll();

    Category findById(Long id);

    Category save(Category category);

    void delete(Long id);

    List<Category> findFeatured();



}