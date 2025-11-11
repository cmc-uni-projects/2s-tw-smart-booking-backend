package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.FileUploadResponse;
import com.example.smart_booking_system.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);

        // Tạo URL công khai cho file
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/files/") // Đây là đường dẫn ta sẽ cấu hình ở bước 4
                .path(fileName)
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