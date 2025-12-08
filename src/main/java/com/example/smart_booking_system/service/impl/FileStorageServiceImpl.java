package com.example.smart_booking_system.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.smart_booking_system.exception.InternalServerException;
import com.example.smart_booking_system.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final Cloudinary cloudinary;

    @Override
    public String storeImageFile(MultipartFile file) {
        return uploadToCloudinary(file, "smart_booking_general");
    }

    @Override
    public String storeImageFile(MultipartFile file, String folderName) {
        return uploadToCloudinary(file, folderName);
    }

    private String uploadToCloudinary(MultipartFile file, String folderName) {
        try {
            if (file.isEmpty()) {
                throw new InternalServerException("File rỗng, không thể lưu.");
            }

            if (file.getContentType() == null || !file.getContentType().startsWith("image")) {
                throw new InternalServerException("File không phải hình ảnh hợp lệ.");
            }

            String publicId = UUID.randomUUID().toString();

            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", folderName,
                    "public_id", publicId,
                    "resource_type", "image",

                    // ====== TỐI ƯU HÓA ẢNH BẬC NHẤT 2025 ======
                    "fetch_format", "auto",
                    "quality", "auto:eco",
                    "width", 1200,
                    "crop", "limit",
                    "dpr", "auto",
                    "flags", "progressive"
            );

            Map<String, Object> uploadResult =
                    cloudinary.uploader().upload(file.getInputStream(), params);

            return (String) uploadResult.get("secure_url");

        } catch (IOException e) {
            throw new InternalServerException("Lỗi IO khi upload ảnh lên Cloudinary: " + e.getMessage());
        } catch (Exception e) {
            throw new InternalServerException("Lỗi Cloudinary: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {

    }
}