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

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "folder", required = false) String folder) {

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body("Không có file nào được gửi lên.");
        }

        String targetFolder = (folder != null && !folder.isEmpty())
                ? folder
                : "smart_booking_general";

        // Xử lý nhiều file (kể cả khi có 1 file)
        List<String> urls = fileStorageService.storeImageFiles(files, targetFolder);

        return ResponseEntity.ok(urls);
    }

}