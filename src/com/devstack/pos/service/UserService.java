package com.devstack.pos.service;

import com.devstack.pos.entity.AppUser;
import com.devstack.pos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
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
    
    public boolean checkPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
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
    
    public boolean deleteUser(String email) {
        if (userRepository.existsByEmail(email)) {
            userRepository.deleteById(email);
            return true;
        }
        return false;
    }
}

