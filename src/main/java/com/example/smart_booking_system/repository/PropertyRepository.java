package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.enums.PropertyType;
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

    // ========================================================================================
    // 1. MASTER QUERY: TÌM KIẾM + LỌC (Thành phố, Giá, Loại, Tiện nghi) + PHÂN TRANG + CHECK TRỐNG
    // ========================================================================================
    @Query("""
        SELECT DISTINCT p FROM Property p
        JOIN p.rooms r
        WHERE p.isActive = true
        AND p.propertyStatus = com.example.smart_booking_system.enums.PropertyStatus.APPROVE
        AND r.isActive = true
        AND (:keyword IS NULL OR :keyword = '' OR (
             LOWER(p.propertyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(p.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(p.district) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ))
        AND (:cities IS NULL OR p.city IN :cities)
        AND (:types IS NULL OR p.propertyType IN :types)
        AND (:minRating IS NULL OR p.rating >= :minRating)
        AND (:minPrice IS NULL OR r.pricePerNight >= :minPrice)
        AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice)
        AND (:guestCount IS NULL OR r.capacity >= :guestCount)
        AND (:amenities IS NULL OR (
            SELECT COUNT(DISTINCT pa.amenity.amenityId)
            FROM PropertyAmenity pa
            WHERE pa.property = p
            AND pa.amenity.amenityName IN :amenities
            AND pa.active = true  
        ) = :amenityCount)
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
    Page<Property> searchPropertiesWithFilter(
            @Param("keyword") String keyword,
            @Param("cities") List<String> cities,
            @Param("types") List<PropertyType> types,
            @Param("amenities") List<String> amenities,
            @Param("amenityCount") Long amenityCount,
            @Param("minRating") BigDecimal minRating,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("guestCount") Integer guestCount,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            Pageable pageable
    );

    // ============================================================
    // 2. DANH SÁCH NỔI BẬT (TOP 10 RATING CAO NHẤT)
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

    List<Property> findByOwner_UserIdAndPropertyStatus(String ownerId, PropertyStatus status);

    long countByOwner_UserId(String ownerId);

    // ============================================================
    // 4. CÁC QUERY KHÁC (GEOLOCATION, VALIDATION)
    // ============================================================

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

    @Query("""
        SELECT DISTINCT p FROM Property p
        JOIN p.rooms r
        WHERE p.isActive = true
        AND p.propertyStatus = com.example.smart_booking_system.enums.PropertyStatus.APPROVE
        AND r.isActive = true
        AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.propertyName) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    List<Property> searchProperties(@Param("keyword") String keyword);
}