// src/main/java/com/devstack/pos/service/CustomUserDetailsService.java
package com.devstack.pos.config;

import com.devstack.pos.entity.AppUser;
import com.devstack.pos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Load your AppUser entity
        AppUser appUser = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // 2. Map your AppUser to the Spring Security UserDetails object
        String role = appUser.getRoles().stream()
            .findFirst()
            .map(r -> "ROLE_" + r.getName()) // Roles must be prefixed with 'ROLE_'
            .orElse("ROLE_USER"); 
            
        return new User(
            appUser.getEmail(), 
            appUser.getPassword(), 
            Collections.singletonList(() -> role) 
        );
    }
}