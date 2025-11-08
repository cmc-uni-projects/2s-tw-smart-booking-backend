package com.example.smart_booking_system.config;

import com.example.smart_booking_system.entity.Role;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.repository.RoleRepository;
import com.example.smart_booking_system.repository.UserRepository; // THÊM MỚI
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder; // THÊM MỚI
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set; // THÊM MỚI
import java.util.UUID; // THÊM MỚI

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository; // THÊM MỚI
    private final PasswordEncoder passwordEncoder; // THÊM MỚI

    @PostConstruct
    public void init() {
        // 1. Khởi tạo Roles
        initRoles();

        // 2. Khởi tạo Users
        initDefaultUsers();
    }

    private void initRoles() {
        List<String> defaultRoles = List.of("CUSTOMER", "ADMIN", "OWNER");

        for (String roleName : defaultRoles) {
            roleRepository.findByRoleName(roleName)
                    .or(() -> {
                        Role role = new Role();
                        role.setRoleName(roleName);
                        roleRepository.save(role);
                        System.out.println("✅ Created default role: " + roleName);
                        return java.util.Optional.of(role);
                    });
        }
    }

    // THÊM MỚI HÀM NÀY
    private void initDefaultUsers() {
        createAccountIfNotExists(
                "admin@travelmate.vn",
                "Admin@123",
                "Admin FullName",
                Set.of("ADMIN", "CUSTOMER")
        );
        createAccountIfNotExists(
                "owner@travelmate.vn",
                "Owner@123",
                "Owner FullName",
                Set.of("OWNER", "CUSTOMER")
        );
        createAccountIfNotExists(
                "customer@travelmate.vn",
                "Customer@123",
                "Customer FullName",
                Set.of("CUSTOMER")
        );
    }

    // THÊM MỚI HÀM HELPER NÀY
    private void createAccountIfNotExists(String email, String rawPassword, String fullName, Set<String> roleNames) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setIsEmailVerified(true); // Đã xác minh để test
        user.setStatus("ACTIVE"); // Active để test
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        Set<Role> roles = roleRepository.findAll().stream()
                .filter(role -> roleNames.contains(role.getRoleName()))
                .collect(java.util.stream.Collectors.toSet());
        user.setRoles(roles);

        userRepository.save(user);
        System.out.println("✅ Created default user: " + email);
    }
}