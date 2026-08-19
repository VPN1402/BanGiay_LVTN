package com.example.LVTN.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true)
    private String email;

    private String password;

    private String phone;

    private Integer status = 1;

    private String address;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;


    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

}
