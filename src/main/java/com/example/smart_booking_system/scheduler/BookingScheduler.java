package com.example.smart_booking_system.scheduler;

import com.example.smart_booking_system.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingScheduler {

    private final BookingService bookingService;

    // Chạy mỗi 1 phút (60.000 ms) để quét đơn quá hạn
    @Scheduled(fixedRate = 60000)
    public void checkExpiredBookings() {
        bookingService.scanAndCancelExpiredBookings();
    }
}