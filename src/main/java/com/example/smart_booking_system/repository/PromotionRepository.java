package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.Promotion;
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

    List<Promotion> findByIsActiveTrue();

    @Query("""
        SELECT p FROM Promotion p
        WHERE p.code = :code
        AND p.isActive = true
        AND p.startDate <= :now
        AND p.endDate >= :now
        AND (p.usageLimit IS NULL OR p.usageCount < p.usageLimit)
    """)
    Optional<Promotion> findValidPromotion(@Param("code") String code, @Param("now") LocalDate now);
}