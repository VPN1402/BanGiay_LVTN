package com.example.LVTN.security;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // 1. Các tài nguyên công khai ai cũng được truy cập
                        .requestMatchers(
                                "/auth/**",
                                "/css/**",
                                "/js/**",
                                "/admin/css/**",
                                "/admin/js/**",
                                "/register",
                                "/cart/**"
                        ).permitAll()

                        // 2. Phân quyền trang chiến lược của Tổng Giám Đốc (CEO)
                        .requestMatchers("/ceo/**").hasAnyAuthority("ROLE_CEO", "ROLE_ADMIN")

                        // 3. Phân quyền trang quản lý tồn kho & quét mã QR dành cho Thủ Kho
                        .requestMatchers("/admin/inventory/**", "/admin/import/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_WAREHOUSE")

                        // 4. Các trang vận hành còn lại của admin (quản lý đơn hàng, doanh thu, nhân sự)
                        .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")

                        // 5. Phân quyền phân hệ dành riêng cho đối tác cung ứng hàng hóa
                        .requestMatchers("/supplier/**").hasAuthority("ROLE_SUPPLIER")

                        // Tất cả các request khác (Trang chủ, xem sản phẩm, mua hàng...) đều công khai
                        .anyRequest().permitAll()
                )

                .formLogin(login -> login
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")

                        // Bộ xử lý điều hướng thông minh sau khi đăng nhập thành công
                        .successHandler((request, response, authentication) -> {

                            Set<String> roles = authentication.getAuthorities().stream()
                                    .map(a -> a.getAuthority())
                                    .collect(Collectors.toSet());

                            System.out.println("Tài khoản đăng nhập thành công với các quyền: " + roles);

                            if (roles.contains("ROLE_ADMIN")) {
                                response.sendRedirect("/admin");
                            } else if (roles.contains("ROLE_CEO")) {
                                response.sendRedirect("/ceo/dashboard"); // Đưa thẳng CEO vào trang phân tích vĩ mô
                            } else if (roles.contains("ROLE_MANAGER")) {
                                response.sendRedirect("/admin"); // Cửa hàng trưởng vào trang quản lý vận hành chung
                            } else if (roles.contains("ROLE_WAREHOUSE")) {
                                response.sendRedirect("/admin/inventory"); // Thủ kho đưa thẳng vào trang quản lý hàng hóa/quét mã QR
                            } else if (roles.contains("ROLE_SUPPLIER")) {
                                response.sendRedirect("/supplier/bid/create");
                            } else {
                                response.sendRedirect("/");
                            }
                        })
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/auth/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}