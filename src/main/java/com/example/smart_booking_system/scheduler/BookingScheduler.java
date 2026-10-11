package com.example.smart_booking_system.scheduler;

import lombok.extern.slf4j.Slf4j;
import com.example.smart_booking_system.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingService bookingService;

    // Chạy mỗi 1 phút (60.000 ms) để quét đơn quá hạn
    @Scheduled(fixedRate = 60000)
    public void checkExpiredBookings() {
        bookingService.scanAndCancelExpiredBookings();
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void sendCheckinReminders() {
        log.info("Sending check-in reminders");
        bookingService.sendCheckinReminders();
    }
}