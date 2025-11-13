package com.example.smart_booking_system.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Lưu file vào thư mục con (ví dụ: "avatars")
     */
    String storeImageFile(MultipartFile file, String subDirectory);

    /**
     * Lưu file vào thư mục gốc (không có thư mục con)
     */
    String storeImageFile(MultipartFile file);

    /**
     * Xóa file vật lý dựa trên đường dẫn tương đối
     * @param relativeFilePath Đường dẫn tương đối (ví dụ: "avatars/abc.png")
     */
    void deleteFile(String relativeFilePath);
}