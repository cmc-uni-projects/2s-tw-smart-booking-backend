// src/main/java/com/example/smart_booking_system/repository/TwoFactorOtpRepository.java

package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.TwoFactorOtp;
import com.example.smart_booking_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TwoFactorOtpRepository extends JpaRepository<TwoFactorOtp, Long> {

    // Tìm kiếm OTP hợp lệ cho người dùng
    Optional<TwoFactorOtp> findByUserAndExpirationTimeAfter(User user, Instant currentTime);

    // Xóa OTP của người dùng
    void deleteByUser(User user);
}