package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.FileUploadResponse;
import com.example.smart_booking_system.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) {

        String fileUrl;

        // Nếu client gửi kèm tên folder thì dùng, không thì dùng mặc định
        if (folder != null && !folder.isEmpty()) {
            fileUrl = fileStorageService.storeImageFile(file, folder);
        } else {
            fileUrl = fileStorageService.storeImageFile(file);
        }

        // Tạo response trả về URL Cloudinary trực tiếp
        FileUploadResponse response = new FileUploadResponse(
                fileUrl,        // fileDownloadUri (bây giờ là link Cloudinary)
                fileUrl,        // fileName (dùng luôn link hoặc bạn có thể tách tên ra nếu muốn)
                file.getSize(),
                file.getContentType()
        );

        return ResponseEntity.ok(response);
    }
}