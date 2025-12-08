package com.example.smart_booking_system.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {

    String storeImageFile(MultipartFile file);

    String storeImageFile(MultipartFile file, String folderName);

    List<String> storeImageFiles(MultipartFile[] files, String folderName);

    void deleteFile(String fileUrl);
}
