package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Promotion;
import com.example.smart_booking_system.enums.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    // 1. Check code tồn tại (Chỉ giữ lại 1 cái này)
    boolean existsByCode(String code);

    // 2. Tìm theo trạng thái
    List<Promotion> findByStatus(PromotionStatus status);

    // 3. Tìm mã cụ thể để validate (Check cả hạn dùng + số lượng)
    @Query("""
        SELECT p FROM Promotion p
        WHERE p.code = :code
        AND p.status = com.example.smart_booking_system.enums.PromotionStatus.ACTIVE
        AND p.startDate <= :now
        AND p.endDate >= :now
        AND (p.usageLimit IS NULL OR p.usageCount < p.usageLimit)
    """)
    Optional<Promotion> findValidPromotion(@Param("code") String code, @Param("now") LocalDateTime now);

    // 4. Hàm cho Scheduler (Tìm mã hết hạn để update status)
    List<Promotion> findByStatusAndEndDateBefore(PromotionStatus status, LocalDateTime date);

    // 5. Lấy danh sách mã đang ACTIVE (cơ bản)
    @Query("""
        SELECT p FROM Promotion p
        WHERE p.status = com.example.smart_booking_system.enums.PromotionStatus.ACTIVE
        AND p.startDate <= :now
        AND p.endDate >= :now
        AND (p.usageLimit IS NULL OR COALESCE(p.usageCount, 0) < p.usageLimit) 
    """)
    List<Promotion> findAvailablePromotions(@Param("now") LocalDateTime now);

    // ==========================================
    // CÁC HÀM MỚI CHO OWNER & PROPERTY
    // ==========================================

    // 6. Lấy mã riêng của một Property cụ thể (Dùng cho Owner xem quản lý)
    // Yêu cầu: Trong Entity Promotion phải có quan hệ "private Property property;"
    List<Promotion> findByProperty_PropertyId(int propertyId);

    // 7. Lấy mã toàn sàn (Dùng cho Admin xem quản lý)
    List<Promotion> findByPropertyIsNull();

    // 8. Tìm mã khuyến mãi áp dụng được cho 1 Property cụ thể
    // (Bao gồm: Mã toàn sàn + Mã riêng của Property đó)
    // Dùng khi User xem chi tiết phòng hoặc chuẩn bị đặt phòng
    @Query("""
        SELECT p FROM Promotion p
        WHERE p.status = com.example.smart_booking_system.enums.PromotionStatus.ACTIVE
        AND p.startDate <= :now
        AND p.endDate >= :now
        AND (p.usageLimit IS NULL OR COALESCE(p.usageCount, 0) < p.usageLimit)
        AND (p.property IS NULL OR p.property.propertyId = :propertyId)
    """)
    List<Promotion> findPromotionsForProperty(@Param("propertyId") int propertyId, @Param("now") LocalDateTime now);
}