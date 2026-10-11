package com.example.smart_booking_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling // ✅ Bật tính năng lập lịch
public class SmartBookingSystemApplication {
    /** Business time zone: LocalDateTime values (DB, API, schedulers) are Vietnam wall-clock time. */
    public static final String TIME_ZONE = "Asia/Ho_Chi_Minh";

    public static void main(String[] args) {
        // Same clock everywhere, whatever the host/container zone is (Docker images run in UTC).
        TimeZone.setDefault(TimeZone.getTimeZone(TIME_ZONE));
        SpringApplication.run(SmartBookingSystemApplication.class, args);
    }
}