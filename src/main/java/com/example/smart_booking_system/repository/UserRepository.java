package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find user by verification token
     */
    Optional<User> findByVerificationToken(String token);

    /**
     * Find user by reset password token
     */
    Optional<User> findByResetPasswordToken(String token);

    /**
     * Find users by status
     */
    List<User> findByStatus(String status);

    /**
     * Find users with specific role
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    /**
     * Delete expired verification tokens
     */
    @Query("UPDATE User u SET u.verificationToken = NULL, u.verificationTokenExpiry = NULL " +
            "WHERE u.verificationTokenExpiry < :now")
    void deleteExpiredVerificationTokens(@Param("now") LocalDateTime now);

    /**
     * Delete expired reset password tokens
     */
    @Query("UPDATE User u SET u.resetPasswordToken = NULL, u.resetPasswordTokenExpiry = NULL " +
            "WHERE u.resetPasswordTokenExpiry < :now")
    void deleteExpiredResetPasswordTokens(@Param("now") LocalDateTime now);

    /**
     * Count users by status
     */
    long countByStatus(String status);

    /**
     * Search users by full name or email
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@Param("keyword") String keyword);
}
