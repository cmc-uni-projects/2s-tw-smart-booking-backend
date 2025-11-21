package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Promotion;
import com.example.smart_booking_system.enums.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    boolean existsByCode(String code);

    // Tìm tất cả mã theo trạng thái
    List<Promotion> findByStatus(PromotionStatus status);

    // Query tìm mã hợp lệ để áp dụng khi Booking
    @Query("""
        SELECT p FROM Promotion p
        WHERE p.code = :code
        AND p.status = 'ACTIVE'  
        AND p.startDate <= :now
        AND p.endDate >= :now
        AND (p.usageLimit IS NULL OR p.usageCount < p.usageLimit)
    """)
    Optional<Promotion> findValidPromotion(@Param("code") String code, @Param("now") LocalDate now);

    // Tìm các mã đang ACTIVE nhưng ngày kết thúc đã qua (endDate < hôm nay)
    List<Promotion> findByStatusAndEndDateBefore(com.example.smart_booking_system.enums.PromotionStatus status, java.time.LocalDate date);
}