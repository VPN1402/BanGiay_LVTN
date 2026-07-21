package com.example.LVTN.security;

import com.example.LVTN.entity.User;
import com.example.LVTN.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().getRoleName());


        boolean isActive = (user.getStatus() != null && user.getStatus() == 1);

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                isActive,
                true,      // accountNonExpired
                true,      // credentialsNonExpired
                true,      // accountNonLocked
                Collections.singletonList(authority)
        );
    }
}