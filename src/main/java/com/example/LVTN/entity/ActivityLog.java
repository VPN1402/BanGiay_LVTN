package com.example.LVTN.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cho phép nullable = true để tránh lỗi khi thao tác hệ thống không gắn ID người dùng
    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "role_name")
    private String roleName;

    @Column(name = "action_name")
    private String actionName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Tự động gán thời gian hiện tại trước khi lưu vào DB nếu chưa truyền
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}