package com.devstack.pos.service;

import com.devstack.pos.entity.AppUser;
import com.devstack.pos.entity.Role;
import com.devstack.pos.repository.UserRepository;
import com.devstack.pos.util.JwtUtil;
import com.devstack.pos.view.tm.TokenRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public boolean saveUser(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            return false;
        }
        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setPassword(passwordEncoder.encode(password));
        userRepository.save(appUser);
        return true;
    }

    public AppUser findUser(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public String checkPassword(String email, String password) {
        try {

            System.out.println("checkPassword(String email, String password)"+email+" "+password);
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

            // Load user details
            AppUser user = findUser(email);
            if (user == null) {
                System.out.println("User not found"+email);
                throw new RuntimeException("User not found");

            }

            // Generate JWT
            TokenRequest tokenRequest = TokenRequest.builder()
                    .role(user.getRoles().stream()
                            .findFirst() // Get the first role available
                            .map(Role::getName) // Extract the role name
                            .orElseThrow(() -> new RuntimeException("No roles found for the user"))).username(user.getEmail())
                    .now(LocalDateTime.now())
                    .build();
            return jwtUtil.createJwtToken(tokenRequest);
        } catch (Exception e) {
            System.out.println("Exception e = new RuntimeException(\"Invalid username or password\");"+e.getMessage() );
            return null;
        }

    }

    public boolean updateUser(String email, String newPassword) {
        Optional<AppUser> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            AppUser appUser = userOpt.get();
            appUser.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(appUser);
            return true;
        }
        return false;
    }

}

