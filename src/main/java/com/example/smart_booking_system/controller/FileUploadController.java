package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.FileUploadResponse;
import com.example.smart_booking_system.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;


    @Value("${file.static-url-prefix}")
    private String staticUrlPrefix;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {

        // Gọi hàm 1 tham số (lưu vào thư mục gốc)
        String fileName = fileStorageService.storeImageFile(file);


        // Tạo URL công khai cho file (ví dụ: http://.../images/abc.png)
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(staticUrlPrefix + "/") // Dùng prefix (ví dụ: /images/)
                .path(fileName)              // Tên file (ví dụ: abc.png)
                .toUriString();

        FileUploadResponse response = new FileUploadResponse(
                fileDownloadUri,
                fileName,
                file.getSize(),
                file.getContentType()
        );

        return ResponseEntity.ok(response);
    }
}