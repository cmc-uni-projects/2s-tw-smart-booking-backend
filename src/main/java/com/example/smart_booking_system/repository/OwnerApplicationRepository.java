package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.OwnerApplication;
import com.example.smart_booking_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OwnerApplicationRepository extends JpaRepository<OwnerApplication, UUID> {

    /**
     * Find applications by user
     */
    List<OwnerApplication> findByUser(User user);

    /**
     * Find applications by user ID
     */
    List<OwnerApplication> findByUserUserId(UUID userId);

    /**
     * Find applications by status
     */
    List<OwnerApplication> findByStatus(String status);

    /**
     * Find applications by status ordered by creation date
     */
    List<OwnerApplication> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Check if user has pending application
     */
    boolean existsByUserUserIdAndStatus(UUID userId, String status);

    /**
     * Count applications by status
     */
    long countByStatus(String status);

    /**
     * Find latest application by user
     */
    Optional<OwnerApplication> findFirstByUserUserIdOrderByCreatedAtDesc(UUID userId);
}