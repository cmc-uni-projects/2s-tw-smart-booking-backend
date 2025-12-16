package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.TwoFactorOtp;
import com.example.smart_booking_system.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID; // Không cần thiết nếu UserID là String
// import java.util.UUID; // Xóa import này nếu không dùng

@Repository
@Transactional
public interface TwoFactorOtpRepository extends JpaRepository<TwoFactorOtp, Long> {

    // Phương thức kiểm tra OTP và thời gian hết hạn (Nếu được dùng)
    Optional<TwoFactorOtp> findByUserAndExpirationTimeAfter(User user, Instant currentTime);

    // Xóa OTP
    void deleteByUser(User user);

    // Tìm kiếm bằng đối tượng User (Phương thức gốc, chúng ta cố gắng tránh dùng)
    Optional<TwoFactorOtp> findByUser(User user);

    /**
     * ✅ PHƯƠNG THỨC MỚI: Tìm kiếm OTP bằng ID của User
     * Phương thức này truy vấn trực tiếp cột khóa ngoại userId
     */
    Optional<TwoFactorOtp> findByUser_UserId(String userId);
}