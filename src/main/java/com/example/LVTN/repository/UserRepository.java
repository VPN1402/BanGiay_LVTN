package com.example.LVTN.repository;

import com.example.LVTN.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Kiểm tra xem email đã tồn tại chưa (Dùng cho logic Đăng ký)
    boolean existsByEmail(String email);

    // Tìm kiếm người dùng theo Email (Dùng cho logic Đăng nhập)
    // Dùng Optional để tránh lỗi NullPointerException nếu không tìm thấy
    Optional<User> findByEmail(String email);
}