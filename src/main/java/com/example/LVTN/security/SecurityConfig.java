package com.example.LVTN.security;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Dùng để mã hóa mật khẩu
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Tắt để test local cho dễ
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/css/**", "/js/**", "/register").permitAll() // Các trang không cần đăng nhập
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN") // Chỉ admin mới vào được
                        .anyRequest().authenticated() // Còn lại phải đăng nhập hết
                )
                .formLogin(login -> login
                        .loginPage("/auth/login") // Trang login của bạn
                        .loginProcessingUrl("/login") // Đường dẫn mà form POST đến
                        .usernameParameter("email") // Vì bạn đã sửa html thành email
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true) // Thành công thì về trang chủ
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // Đường dẫn để kích hoạt đăng xuất
                        .logoutSuccessUrl("/auth/login?logout") // Đăng xuất xong thì quay về trang login
                        .invalidateHttpSession(true) // Xóa session hiện tại
                        .deleteCookies("JSESSIONID") // Xóa cookie trình duyệt
                        .permitAll()
                );

        return http.build();
    }
}