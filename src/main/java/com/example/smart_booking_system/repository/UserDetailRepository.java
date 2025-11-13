package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.UserDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDetailRepository extends JpaRepository<UserDetail, Integer> {

    // === CẬP NHẬT TRUY VẤN NÀY ===
    // Tìm UserDetail bằng userId VÀ ĐANG ACTIVE
    @Query("SELECT ud FROM UserDetail ud WHERE ud.user.userId = :userId AND ud.isActive = true")
    Optional<UserDetail> findActiveByUserId(@Param("userId") String userId);

    // === CẬP NHẬT TRUY VẤN NÀY ===
    // Kiểm tra tồn tại bằng userId VÀ ĐANG ACTIVE
    @Query("SELECT COUNT(ud) > 0 FROM UserDetail ud WHERE ud.user.userId = :userId AND ud.isActive = true")
    boolean existsActiveByUserId(@Param("userId") String userId);

    // Vẫn giữ lại hàm tìm bất kể trạng thái (dùng nội bộ nếu cần)
    Optional<UserDetail> findByUserUserId(String userId);
}