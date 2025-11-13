package com.example.smart_booking_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // Lấy đường dẫn thư mục vật lý từ properties
    @Value("${file.upload-dir}")
    private String uploadDir;

    // Lấy đường dẫn URL ảo từ properties
    @Value("${file.static-path-pattern}")
    private String staticPathPattern; // Ví dụ: /images/**

    /**
     * Cấu hình resource handler để phục vụ file tĩnh (ảnh)
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn thư mục upload
        Path uploadPath = Paths.get(uploadDir);
        String absoluteUploadPath = uploadPath.toFile().getAbsolutePath();

        // Ánh xạ đường dẫn ảo (ví dụ: /images/**) tới thư mục thật
        registry
                .addResourceHandler(staticPathPattern)
                .addResourceLocations("file:/" + absoluteUploadPath + "/");
    }
}