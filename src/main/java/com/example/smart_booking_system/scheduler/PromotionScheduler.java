package com.example.smart_booking_system.scheduler;

import com.example.smart_booking_system.entity.Promotion;
import com.example.smart_booking_system.enums.PromotionStatus;
import com.example.smart_booking_system.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PromotionScheduler {

    private final PromotionRepository promotionRepository;

    /**
     * Chạy định kỳ mỗi ngày vào lúc 00:00:00
     * Nhiệm vụ: Quét toàn bộ mã đang ACTIVE, nếu hết hạn thì chuyển sang EXPIRED
     */
    @Scheduled(cron = "0 0 0 * * ?") // Cron expression: Giây Phút Giờ Ngày Tháng Thứ
    @Transactional
    public void autoExpirePromotions() {
        LocalDate today = LocalDate.now();

        // 1. Tìm các mã đang ACTIVE mà ngày kết thúc < hôm nay
        List<Promotion> expiredPromotions = promotionRepository.findByStatusAndEndDateBefore(PromotionStatus.ACTIVE, today);

        if (!expiredPromotions.isEmpty()) {
            // 2. Cập nhật trạng thái sang EXPIRED
            for (Promotion p : expiredPromotions) {
                p.setStatus(PromotionStatus.EXPIRED);
            }

            // 3. Lưu lại vào DB
            promotionRepository.saveAll(expiredPromotions);

            System.out.println("CreateJob: Đã cập nhật trạng thái EXPIRED cho " + expiredPromotions.size() + " mã khuyến mãi.");
        }
    }
}