package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.exception.InternalServerException;
import com.example.smart_booking_system.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageServiceImpl(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            // SỬA LỖI 1: Chỉ truyền String message
            throw new InternalServerException("Không thể tạo thư mục để lưu trữ file upload: " + ex.getMessage());
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        // Tạo tên file duy nhất
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        try {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        } catch (Exception e) {
            fileExtension = "";
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // Kiểm tra ký tự không hợp lệ
            if (fileName.contains("..")) {
                throw new InternalServerException("Tên file chứa ký tự không hợp lệ " + fileName);
            }

            // Sao chép file vào thư mục đích
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            return fileName;
        } catch (IOException ex) {
            // SỬA LỖI 2: Chỉ truyền String message
            throw new InternalServerException("Không thể lưu trữ file " + fileName + ". Vui lòng thử lại!: " + ex.getMessage());
        }
    }
}