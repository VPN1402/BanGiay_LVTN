package com.example.LVTN.service;

import com.example.LVTN.entity.Brand;

import java.util.List;

public interface BrandService {

    List<Brand> findAll();

    Brand findById(Long id);

    Brand save(Brand brand);

    void delete(Long id);
}