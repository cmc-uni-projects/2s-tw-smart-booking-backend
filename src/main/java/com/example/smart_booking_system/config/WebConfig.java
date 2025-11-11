package com.example.smart_booking_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry; // <-- THÊM IMPORT NÀY
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cấu hình này của bạn để phục vụ file upload
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    // === THÊM HÀM NÀY VÀO FILE CỦA BẠN ===
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**") // Áp dụng cho tất cả các đường dẫn API
                .allowedOrigins("http://localhost:5173") // Cho phép origin của frontend
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // Các phương thức cho phép
                .allowedHeaders("*") // Cho phép tất cả các header (bao gồm cả Authorization)
                .allowCredentials(true); // Cho phép gửi cookie hoặc token
    }
    // === KẾT THÚC PHẦN THÊM MỚI ===
}