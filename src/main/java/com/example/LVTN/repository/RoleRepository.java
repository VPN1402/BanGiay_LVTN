package com.example.LVTN.repository;

import com.example.LVTN.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // Tìm kiếm theo thuộc tính roleName đã khai báo trong Entity
    Role findByRoleName(String roleName);
}