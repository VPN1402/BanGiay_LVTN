package com.example.LVTN.service.impl;

import com.example.LVTN.entity.ActivityLog;
import com.example.LVTN.entity.User;
import com.example.LVTN.repository.ActivityLogRepository;
import com.example.LVTN.repository.UserRepository;
import com.example.LVTN.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void log(Long userId, String fullName, String roleName, String actionName, String description, HttpServletRequest request) {
        ActivityLog log = new ActivityLog();

        // 1. Tự động lấy User đang đăng nhập nếu userId = null
        if (userId == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String currentEmail = auth.getName(); // Lấy email từ Spring Security
                User currentUser = userRepository.findByEmail(currentEmail).orElse(null);

                if (currentUser != null) {
                    userId = currentUser.getId();

                    // Lấy fullName, nếu chưa có thì lấy email làm tên hiển thị
                    if (fullName == null || "ADMIN".equalsIgnoreCase(fullName)) {
                        fullName = (currentUser.getFullName() != null && !currentUser.getFullName().isBlank())
                                ? currentUser.getFullName()
                                : currentUser.getEmail(); // SỬA Ở ĐÂY: getEmail() thay vì getUsername()
                    }
                }
            }
        }

        // 2. Tự động trích xuất Request nếu bị null
        if (request == null) {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();
            }
        }

        // 3. Lấy IP Client
        String clientIp = getClientIp(request);

        // 4. Gán dữ liệu và lưu
        log.setUserId(userId);
        log.setFullName(fullName);
        log.setRoleName(roleName);
        log.setActionName(actionName);
        log.setDescription(description);
        log.setIpAddress(clientIp);
        log.setCreatedAt(LocalDateTime.now());

        activityLogRepository.save(log);
    }

    @Override
    public void log(Long userId, String fullName, String roleName, String actionName, String description) {
        log(userId, fullName, roleName, actionName, description, null);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}