package com.example.smart_booking_system.dto;

import com.example.smart_booking_system.entity.Promotion;
import com.example.smart_booking_system.enums.DiscountType;
import com.example.smart_booking_system.enums.PromotionStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class PromotionResponseDTO {

    private Integer promotionId;
    private String code;
    private String description;



    private String discountDetail;
    private Integer usageLimit;
    private Integer usageCount;
    private LocalDateTime endDate;
    private LocalDateTime startDate;
    private PromotionStatus status;
    private DiscountType discountType;

    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minBookingAmount;
    private String bannerUrl;

    public PromotionResponseDTO(Promotion p) {
        this.promotionId = p.getPromotionId();
        this.code = p.getCode();
        this.description = p.getDescription();
        this.usageLimit = p.getUsageLimit();
        this.usageCount = p.getUsageCount();
        this.endDate = LocalDateTime.from(p.getEndDate());
        this.startDate = LocalDateTime.from(p.getStartDate());
        this.status = p.getStatus();
        this.discountType = p.getDiscountType();

        this.discountValue = p.getDiscountValue();
        this.maxDiscountAmount = p.getMaxDiscountAmount();
        this.minBookingAmount = p.getMinBookingAmount();

        this.discountDetail = formatDiscountDetail(p);
        this.bannerUrl = p.getBannerUrl();
    }

    // --- HELPER METHODS ---
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0đ";
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat currencyVN = NumberFormat.getCurrencyInstance(localeVN);
        return currencyVN.format(amount);
    }

    private String formatPercent(BigDecimal percent) {
        if (percent == null) return "0%";
        return percent.stripTrailingZeros().toPlainString() + "%";
    }

    private String formatDiscountDetail(Promotion p) {
        if (p.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            return formatCurrency(p.getDiscountValue());
        } else {
            String percentStr = formatPercent(p.getDiscountValue());
            if (p.getMaxDiscountAmount() != null && p.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                return percentStr + " (Tối đa " + formatCurrency(p.getMaxDiscountAmount()) + ")";
            } else {
                return percentStr;
            }
        }
    }
}