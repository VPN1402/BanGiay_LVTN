package com.example.LVTN.service;

import jakarta.servlet.http.HttpServletRequest;

public interface ActivityLogService {

    // Hàm đầy đủ có Request để lấy IP
    void log(Long userId, String fullName, String roleName, String actionName, String description, HttpServletRequest request);

    // Hàm rút gọn khi không truyền request (tự gán IP mặc định là 127.0.0.1)
    void log(Long userId, String fullName, String roleName, String actionName, String description);
}