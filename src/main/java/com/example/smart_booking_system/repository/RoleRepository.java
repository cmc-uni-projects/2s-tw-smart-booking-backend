package com.example.smart_booking_system.repository;


import com.example.smart_booking_system.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    /**
     * Find role by role name
     */
    Optional<Role> findByRoleName(String roleName);

    /**
     * Check if role exists by name
     */
    boolean existsByRoleName(String roleName);
}
