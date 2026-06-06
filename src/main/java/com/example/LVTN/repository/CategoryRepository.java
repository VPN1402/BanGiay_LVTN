package com.example.LVTN.repository;

import com.example.LVTN.entity.Category;
import com.example.LVTN.entity.Product;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByFeaturedTrue();

    Example<? extends Category> id(Long id);

}