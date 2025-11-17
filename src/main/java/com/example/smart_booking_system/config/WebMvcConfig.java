package com.example.smart_booking_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.static-path-pattern}")
    private String staticPathPattern;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Tạo đường dẫn tuyệt đối tới thư mục ./uploads
        String resourceLocation = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();

        // Log ra để kiểm tra khi chạy
        System.out.println("================================================");
        System.out.println("Mapping URL: " + staticPathPattern);
        System.out.println("To Path:     " + resourceLocation);
        System.out.println("================================================");

        // 2. Đăng ký handler
        registry.addResourceHandler(staticPathPattern)
                .addResourceLocations(resourceLocation);
    }
}