package com.example.LVTN.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên danh mục  Phải nhập không được để trống")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String thumbnail;
    private Boolean featured;


    @OneToMany(mappedBy = "category",fetch = FetchType.LAZY)
    private List<Product> products;
}
