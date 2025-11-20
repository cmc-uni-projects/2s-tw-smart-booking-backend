package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.enums.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Integer> {

    // ============================================================
    // 1. TÌM KIẾM NÂNG CAO (City + Keyword)
    // ============================================================
    // Cập nhật: Thêm JOIN Room để đảm bảo có phòng mới hiện
    @Query("""
        SELECT DISTINCT p FROM Property p
        JOIN Room r ON r.propertyId = p
        WHERE 
        p.isActive = true 
        AND p.propertyStatus = com.example.smart_booking_system.enums.PropertyStatus.APPROVE
        AND r.isActive = true
        AND (:city IS NULL OR :city = '' OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%')))
        AND (:keyword IS NULL OR :keyword = '' OR (
            LOWER(p.propertyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(CAST(p.propertyType AS string)) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ))
    """)
    List<Property> searchProperties(@Param("city") String city, @Param("keyword") String keyword);

    // ============================================================
    // 2. DANH SÁCH NỔI BẬT (Native Query)
    // ============================================================
    // Đã cập nhật: Chỉ lấy KS đã Active và Approved
    @Query(
            value = "SELECT * FROM properties WHERE isActive = TRUE AND propertyStatus = 'APPROVE' ORDER BY rating DESC LIMIT 10",
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

    // ============================================================
    // 4. TÌM KIẾM CHÍNH (HeroSection dùng hàm này)
    // ============================================================
    // ✅ Đã sửa lại theo yêu cầu:
    // - Dùng INNER JOIN (JOIN) thay vì LEFT JOIN: Bắt buộc KS phải có ít nhất 1 phòng.
    // - Thêm điều kiện `r.isActive = true`: Phòng đó phải đang hoạt động.
    // - Kết quả: Nếu KS không có phòng hoặc toàn bộ phòng đang ẩn -> Không hiển thị.
    @Query("SELECT DISTINCT p FROM Property p " +
            "JOIN Room r ON r.propertyId = p " +
            "WHERE p.isActive = true " +
            "AND p.propertyStatus = com.example.smart_booking_system.enums.PropertyStatus.APPROVE " +
            "AND r.isActive = true " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(p.propertyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(p.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(p.country) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Property> searchProperties(@Param("keyword") String keyword);

}