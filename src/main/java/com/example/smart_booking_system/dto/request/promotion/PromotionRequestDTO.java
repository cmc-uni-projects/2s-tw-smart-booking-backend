package com.example.smart_booking_system.dto.request.promotion;

import com.example.smart_booking_system.enums.DiscountType;
import com.example.smart_booking_system.enums.PromotionStatus;
import com.example.smart_booking_system.enums.MembershipRank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PromotionRequestDTO {

    @NotBlank(message = "{validation.promotion.codeRequired}")
    private String code;

    private String description;

    @NotNull(message = "{validation.promotion.typeRequired}")
    private DiscountType discountType;

    private PromotionStatus status;

    private MembershipRank minMembershipRank;

    @NotNull(message = "{validation.promotion.valueRequired}")
    @Min(value = 0)
    private BigDecimal discountValue;

    @NotNull(message = "{validation.promotion.startRequired}")
    private LocalDateTime startDate;

    @NotNull(message = "{validation.promotion.endRequired}")
    private LocalDateTime endDate;

    @Min(value = 0)
    private BigDecimal minBookingAmount;

    @Min(value = 0)
    private BigDecimal maxDiscountAmount;

    @Min(value = 1)
    private Integer usageLimit;

    private Integer propertyId;
}