package com.example.smart_booking_system.dto.request.promotion;

import com.example.smart_booking_system.enums.PromotionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PromotionRequestDTO {

    @NotBlank(message = "Mã khuyến mãi không được để trống")
    private String code;

    private String description;

    @NotNull(message = "Loại khuyến mãi là bắt buộc")
    private PromotionStatus promotionStatus;

    @NotNull(message = "Giá trị giảm là bắt buộc")
    @Min(value = 0, message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    @NotNull(message = "Ngày bắt đầu là bắt buộc")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc là bắt buộc")
    private LocalDate endDate;

    @Min(value = 0)
    private BigDecimal minBookingAmount;

    @Min(value = 0)
    private BigDecimal maxDiscountAmount;

    @Min(value = 1)
    private Integer usageLimit;
}