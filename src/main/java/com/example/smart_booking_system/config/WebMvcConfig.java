package com.example.smart_booking_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn vật lý chuẩn (file:///...)
        String resourceLocation = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();

        System.out.println("=== CONFIGURING STATIC RESOURCES ===");
        System.out.println("Storage Path: " + resourceLocation);

        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }

        // ✅ 1. Hỗ trợ đường dẫn chuẩn mới (/uploads/**)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);

        // ✅ 2. Hỗ trợ đường dẫn cũ (/images/**) để ảnh Owner hiển thị được
        registry.addResourceHandler("/images/**")
                .addResourceLocations(resourceLocation);

        registry.addResourceHandler("/properties/**")
                .addResourceLocations(resourceLocation + "properties/");

        registry.addResourceHandler("/ratingImage/**")
                .addResourceLocations(resourceLocation + "ratingImage/");
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Áp dụng cho tất cả các endpoint
                // ⚠️ QUAN TRỌNG: Đổi từ allowedOrigins sang allowedOriginPatterns
                .allowedOriginPatterns(
                        "http://localhost:*",              // Cho phép localhost mọi port (3000, 5173, etc)
                        "https://*.vercel.app",            // ✅ CHO PHÉP MỌI TÊN MIỀN VERCEL (bao gồm cả preview link)
                        "https://smart-booking-system-frontend-*.vercel.app" // (Tùy chọn) Pattern cụ thể hơn cho dự án của bạn
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // Cache cấu hình CORS trong 1 giờ
    }

}