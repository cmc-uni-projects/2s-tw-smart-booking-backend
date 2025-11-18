package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.enums.PropertyStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Integer> {

    // === SỬA ĐỔI CÂU QUERY BÊN DƯỚI ===
    @Query("""
        SELECT p FROM Property p
        WHERE 
        (:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%')))
        AND
            (:keyword IS NULL OR (
            LOWER(p.propertyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(CAST(p.propertyType AS string)) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ))
            AND p.isActive = true
            AND p.propertyStatus = com.example.smart_booking_system.enums.PropertyStatus.APPROVE
    """)
    List<Property> searchProperties(@Param("city") String city, @Param("keyword") String keyword);
    // === KẾT THÚC SỬA ĐỔI ===

    @Query(
            value = "SELECT * FROM properties WHERE isActive = TRUE ORDER BY rating DESC LIMIT 10",
            nativeQuery = true
    )
    List<Property> findFeaturedProperties();

    List<Property> findByPropertyStatus(PropertyStatus status);
    @Query("SELECT p FROM Property p WHERE p.owner.userId = :ownerId AND p.propertyStatus <> com.example.smart_booking_system.enums.PropertyStatus.REJECTED")
    List<Property> findAllByOwnerIdAndNotRejected(@Param("ownerId") String ownerId);

    List<Property> findByOwner_UserIdAndPropertyStatus(String ownerId, PropertyStatus status);

    @Query("SELECT DISTINCT p FROM Property p " +
            "JOIN Room r ON r.propertyId = p " + // Vẫn giữ JOIN để đảm bảo khách sạn có phòng
            "WHERE p.isActive = true " +
            // ✅ SỬA LỖI QUAN TRỌNG: Đổi 'APPROVED' thành 'APPROVE' (theo Enum của bạn)
            "AND p.propertyStatus = 'APPROVE' " +
            "AND r.isActive = true " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(p.propertyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(p.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(p.country) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        // ❌ ĐÃ TẠM ẨN ĐIỀU KIỆN CHECK GUESTS
        // "AND (:guests IS NULL OR r.capacity >= :guests)")
    List<Property> searchProperties(
            @Param("keyword") String keyword
            // @Param("guests") Integer guests (Bỏ param này trong query)
    );

}