package com.example.smart_booking_system.config;

import com.example.smart_booking_system.entity.Role;
import com.example.smart_booking_system.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void initRoles() {
        List<String> defaultRoles = List.of("CUSTOMER", "ADMIN", "OWNER");

        for (String roleName : defaultRoles) {
            roleRepository.findByRoleName(roleName)
                    .or(() -> {
                        Role role = new Role();
                        role.setRoleName(roleName);
                        role.setDescription(roleName + " role");
                        roleRepository.save(role);
                        System.out.println("✅ Created default role: " + roleName);
                        return java.util.Optional.of(role);
                    });
        }
    }
}
