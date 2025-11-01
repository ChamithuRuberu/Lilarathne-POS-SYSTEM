package com.devstack.pos.service;
import com.devstack.pos.entity.AppUser;
import com.devstack.pos.entity.Role;
import com.devstack.pos.repository.RoleRepository;
import com.devstack.pos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.beans.Transient;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class SuperAdminService implements CommandLineRunner {


        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) {
            createSuperAdmin();
        }

        @Bean
        @Order(1)
        @Transient
        public CommandLineRunner createSuperAdmin() {

            return args -> {
                if (!userRepository.existsByEmail(("admin@example.com"))) {

                    Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN")
                            .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_SUPER_ADMIN").build()));

                    AppUser appUser = AppUser.builder()
                            .password(passwordEncoder.encode("ADMIN")) // Encode the password
                            .email("admin@example.com") // Provide a valid email
                            .status("ACTIVE")
                            .roles(Set.of(superAdminRole)) // Assign the role as a Set
                            .build();


                    userRepository.save(appUser);
                    System.out.println("Super admin user seeded!");
                }
            };

        }

    @Bean
    @Order(2)
    @Transient
    public CommandLineRunner createCashierRole() {

        return args -> {
            if (!userRepository.existsByEmail(("TEST@example.com"))) {

                Role superAdminRole = roleRepository.findByName("ROLE_CASHIER")
                        .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CASHIER").build()));

                AppUser appUser = AppUser.builder()
                        .password(passwordEncoder.encode("ADMIN")) // Encode the password
                        .email("TEST@example.com") // Provide a valid email
                        .status("ACTIVE")
                        .roles(Set.of(superAdminRole)) // Assign the role as a Set
                        .build();


                userRepository.save(appUser);
                System.out.println("cashier admin user seeded!");
            }
        };

    }

    @Bean
    @Order(3)
    @Transient
    public CommandLineRunner createCashierRole2() {

        return args -> {
            if (!userRepository.existsByEmail(("TEST2@example.com"))) {

                Role superAdminRole = roleRepository.findByName("ROLE_CASHIER")
                        .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CASHIER").build()));

                AppUser appUser = AppUser.builder()
                        .password(passwordEncoder.encode("ADMIN")) // Encode the password
                        .email("TEST2@example.com") // Provide a valid email
                        .status("ACTIVE")
                        .roles(Set.of(superAdminRole)) // Assign the role as a Set
                        .build();


                userRepository.save(appUser);
                System.out.println("cashier admin user seeded!");
            }
        };

    }

    @Bean
    @Order(4)
    @Transient
    public CommandLineRunner createCashierRole3() {

        return args -> {
            if (!userRepository.existsByEmail(("tcdrubeu@gmail.com"))) {

                Role superAdminRole = roleRepository.findByName("ROLE_CASHIER")
                        .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CASHIER").build()));

                AppUser appUser = AppUser.builder()
                        .password(passwordEncoder.encode("ADMIN")) // Encode the password
                        .email("tcdrubeu@gmail.com") // Provide a valid email
                        .status("ACTIVE")
                        .roles(Set.of(superAdminRole)) // Assign the role as a Set
                        .build();


                userRepository.save(appUser);
                System.out.println("cashier admin user seeded!");
            }
        };

    }
}
