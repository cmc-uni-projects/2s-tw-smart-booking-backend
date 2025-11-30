package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Promotion;
import com.example.smart_booking_system.enums.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime; // ✅ Dùng LocalDateTime
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    boolean existsByCode(String code);

    // Tìm theo trạng thái
    List<Promotion> findByStatus(PromotionStatus status);

    // ✅ ĐÃ SỬA: Xóa "AND p.isActive = true" và dùng LocalDateTime
    @Query("""
        SELECT p FROM Promotion p
        WHERE p.code = :code
        AND p.status = com.example.smart_booking_system.enums.PromotionStatus.ACTIVE
        AND p.startDate <= :now
        AND p.endDate >= :now
        AND (p.usageLimit IS NULL OR p.usageCount < p.usageLimit)
    """)
    Optional<Promotion> findValidPromotion(@Param("code") String code, @Param("now") LocalDateTime now);

    // Hàm cho Scheduler (cũng phải dùng LocalDateTime)
    List<Promotion> findByStatusAndEndDateBefore(PromotionStatus status, LocalDateTime date);


    // Lấy list promotion có khả năng dùng được (Active + trong thời gian hiệu lực)
    @Query("""
    SELECT p FROM Promotion p
    WHERE p.status = com.example.smart_booking_system.enums.PromotionStatus.ACTIVE
    AND p.startDate <= :now
    AND p.endDate >= :now
    AND (p.usageLimit IS NULL OR COALESCE(p.usageCount, 0) < p.usageLimit) 
""")
    List<Promotion> findAvailablePromotions(@Param("now") LocalDateTime now);
}