package com.example.smart_booking_system.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Upload file vào folder mặc định
     */
    String storeImageFile(MultipartFile file);

    /**
     * Upload file vào folder tùy chỉnh
     * @param file File ảnh
     * @param folderName Tên thư mục trên Cloudinary
     */
    String storeImageFile(MultipartFile file, String folderName);

    /**
     * Xóa file (nếu cần)
     */
    void deleteFile(String fileUrl);
}