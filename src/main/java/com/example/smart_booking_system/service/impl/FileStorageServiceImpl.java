package com.example.smart_booking_system.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.example.smart_booking_system.exception.InternalServerException;
import com.example.smart_booking_system.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final Cloudinary cloudinary;

    // Upload 1 ảnh - sử dụng folder mặc định
    @Override
    public String storeImageFile(MultipartFile file) {
        return uploadToCloudinary(file, "smart_booking_general");
    }

    // Upload 1 ảnh với folder tùy chọn
    @Override
    public String storeImageFile(MultipartFile file, String folderName) {
        return uploadToCloudinary(file, folderName);
    }

    // Upload nhiều ảnh
    @Override
    public List<String> storeImageFiles(MultipartFile[] files, String folderName) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile f : files) {
            urls.add(uploadToCloudinary(f, folderName));
        }
        return urls;
    }

    // Hàm upload chính
    private String uploadToCloudinary(MultipartFile file, String folderName) {
        try {
            if (file.isEmpty()) {
                throw new InternalServerException("File rỗng, không thể upload.");
            }

            if (file.getContentType() == null || !file.getContentType().startsWith("image")) {
                throw new InternalServerException("File không phải định dạng ảnh.");
            }

            String publicId = UUID.randomUUID().toString();

            // 🔥 Tối ưu ảnh nhẹ nhất có thể
            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", folderName,
                    "public_id", publicId,
                    "resource_type", "image",
                    "transformation", new Transformation()
                            .fetchFormat("auto")        // auto WebP/AVIF → nhẹ nhất
                            .quality("auto:low")        // level tối ưu dung lượng tối đa
                            .width(1024).crop("limit")  // resize max chiều rộng
                            .dpr("auto")                // device pixel ratio
                            .flags("progressive")       // load nhanh hơn
            );

            // Dùng file.getBytes() để tránh lỗi ChannelInputStream
            Map<String, Object> uploadResult =
                    cloudinary.uploader().upload(file.getBytes(), params);

            return (String) uploadResult.get("secure_url");

        } catch (Exception e) {
            throw new InternalServerException("Lỗi Cloudinary: " + e.getMessage());
        }
    }

    // Xóa file Cloudinary
    @Override
    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) return;

            String publicId = extractPublicId(fileUrl);

            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

        } catch (Exception e) {
            throw new InternalServerException("Không thể xóa ảnh: " + e.getMessage());
        }
    }

    // Tách public_id từ URL Cloudinary
    private String extractPublicId(String url) {
        try {
            String afterUpload = url.substring(url.indexOf("/upload/") + 8);
            // → v12345/folder/file.jpg

            String withoutVersion = afterUpload.substring(afterUpload.indexOf("/") + 1);
            // → folder/file.jpg

            return withoutVersion.substring(0, withoutVersion.lastIndexOf("."));
            // → folder/file

        } catch (Exception e) {
            throw new InternalServerException("Không thể extract publicId từ URL: " + url);
        }
    }
}
