package com.example.LVTN.security;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                        // Cho phép mọi tài khoản (kể cả Supplier) tải các file CSS/JS nằm trong thư mục admin này
                        .requestMatchers(
                                "/auth/**",
                                "/css/**",
                                "/js/**",
                                "/admin/css/**",
                                "/admin/js/**",
                                "/register"
                        ).permitAll()

                        .requestMatchers("/cart/**").permitAll()
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/supplier/**").hasAuthority("ROLE_SUPPLIER")
                        .anyRequest().permitAll()
                )

                .formLogin(login -> login
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")

                        .successHandler((request, response, authentication) -> {

                            boolean isAdmin = authentication.getAuthorities()
                                    .stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                            boolean isSupplier = authentication.getAuthorities()
                                    .stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPPLIER"));

                            if (isAdmin) {
                                response.sendRedirect("/admin");
                            } else if (isSupplier) {
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