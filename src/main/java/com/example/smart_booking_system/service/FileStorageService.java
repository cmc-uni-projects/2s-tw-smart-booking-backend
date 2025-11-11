package com.example.smart_booking_system.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /**
     * Lưu file và trả về tên file duy nhất
     * @param file file đầu vào
     * @return Tên file duy nhất đã được lưu
     */
    String storeFile(MultipartFile file);
}