package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.enums.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Integer> {

    // ============================================================
    // 1. TÌM KIẾM NÂNG CAO (Keyword + Guest + Date)
    // ============================================================
    @Query("""
        SELECT DISTINCT p FROM Property p
        JOIN Room r ON r.property = p
        WHERE p.isActive = true
        AND p.propertyStatus = com.example.smart_booking_system.enums.PropertyStatus.APPROVE
        AND r.active = true
        AND (:keyword IS NULL OR :keyword = '' OR (
             LOWER(p.propertyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(p.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(p.country) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ))
        AND (:guestCount IS NULL OR r.capacity >= :guestCount)
        AND (
            :checkInDate IS NULL OR :checkOutDate IS NULL OR
            r.roomId NOT IN (
                SELECT b.room.roomId FROM Booking b
                WHERE b.status IN (
                    com.example.smart_booking_system.enums.BookingStatus.CONFIRMED,
                    com.example.smart_booking_system.enums.BookingStatus.PENDING_PAYMENT
                )
                AND (b.checkInDate < :checkOutDate AND b.checkOutDate > :checkInDate)
            )
        )
    """)
    List<Property> searchProperties(
            @Param("keyword") String keyword,
            @Param("guestCount") Integer guestCount,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    // ============================================================
    // 2. DANH SÁCH NỔI BẬT
    // ============================================================
    @Query(
            value = "SELECT * FROM properties WHERE is_active = TRUE AND property_status = 'APPROVE' ORDER BY rating DESC LIMIT 10",
            nativeQuery = true
    )
    List<Property> findFeaturedProperties();

    // ============================================================
    // 3. QUERY CHO ADMIN/OWNER
    // ============================================================
    List<Property> findByPropertyStatus(PropertyStatus status);

    @Query("SELECT p FROM Property p WHERE p.owner.userId = :ownerId AND p.propertyStatus <> com.example.smart_booking_system.enums.PropertyStatus.REJECTED")
    List<Property> findAllByOwnerIdAndNotRejected(@Param("ownerId") String ownerId);

    // ✅ SỬA LẠI TÊN HÀM CHO ĐÚNG VỚI FIELD 'owner'
    // Field trong Property là 'owner', nên phải dùng 'Owner_UserId'
    List<Property> findByOwner_UserIdAndPropertyStatus(String ownerId, PropertyStatus status);

    // ✅ THÊM HÀM ĐẾM SỐ TÀI SẢN CỦA OWNER (FIX LỖI CỦA BẠN TẠI ĐÂY)
    long countByOwner_UserId(String ownerId);

    // ============================================================
    // 4. CÁC QUERY KHÁC
    // ============================================================

    // Query search cũ
    @Query("SELECT DISTINCT p FROM Property p JOIN Room r ON r.property = p WHERE p.isActive = true AND p.propertyStatus = 'APPROVE' AND r.active = true AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.propertyName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Property> searchProperties(@Param("keyword") String keyword);

    @Query(value = """
    SELECT * FROM properties p 
    WHERE p.isActive = true 
    AND p.propertyStatus = 'APPROVE' 
    AND p.latitude BETWEEN :lat - 0.15 AND :lat + 0.15
    AND p.longitude BETWEEN :lng - 0.15 AND :lng + 0.15
    AND (6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * cos(radians(p.longitude) - radians(:lng)) + 
         sin(radians(:lat)) * sin(radians(p.latitude)))) < :radius
    """, nativeQuery = true)
    List<Property> findNearbyProperties(@Param("lat") double lat,
                                        @Param("lng") double lng,
                                        @Param("radius") double radius);

    boolean existsByPropertyName(String propertyName);

    Page<Property> findByPropertyStatus(PropertyStatus status, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Property p " +
            "JOIN p.rooms r " +
            "WHERE p.isActive = true " +
            "AND p.propertyStatus = :status " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(p.propertyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(p.province) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(p.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:guests IS NULL OR r.capacity >= :guests) " +
            "AND (:minPrice IS NULL OR r.pricePerNight >= :minPrice) " +
            "AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice) " +

            // ✅ LOGIC MỚI: BỎ roomAmount, CHỈ CHECK COUNT = 0
            "AND ( " +
            "   :checkIn IS NULL OR :checkOut IS NULL OR " +
            "   (SELECT COUNT(b) FROM Booking b " +
            "    WHERE b.room = r " +
            "    AND b.status IN (com.example.smart_booking_system.enums.BookingStatus.CONFIRMED, com.example.smart_booking_system.enums.BookingStatus.PENDING_PAYMENT) " +
            "    AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn) " +
            "   ) = 0 " + // Tức là: Không có đơn đặt nào trùng lịch
            ")")
    Page<Property> searchPropertiesAdvanced(
            @Param("status") PropertyStatus status,
            @Param("keyword") String keyword,
            @Param("guests") Integer guests,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
    
}