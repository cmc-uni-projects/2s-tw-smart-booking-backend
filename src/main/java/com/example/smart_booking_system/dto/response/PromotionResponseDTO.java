package com.example.smart_booking_system.dto.response;

import com.example.smart_booking_system.entity.Promotion;
import com.example.smart_booking_system.enums.PromotionStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class PromotionResponseDTO {
    private Integer promotionId;
    private String code;
    private String description;
    private PromotionStatus promotionStatus;
    private BigDecimal discountValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal minBookingAmount;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer usageCount;
    private boolean isActive;

    public PromotionResponseDTO(Promotion p) {
        this.promotionId = p.getPromotionId();
        this.code = p.getCode();
        this.description = p.getDescription();
        this.promotionStatus = p.getPromotionStatus();
        this.discountValue = p.getDiscountValue();
        this.startDate = p.getStartDate();
        this.endDate = p.getEndDate();
        this.minBookingAmount = p.getMinBookingAmount();
        this.maxDiscountAmount = p.getMaxDiscountAmount();
        this.usageLimit = p.getUsageLimit();
        this.usageCount = p.getUsageCount();
        this.isActive = p.isActive();
    }
}