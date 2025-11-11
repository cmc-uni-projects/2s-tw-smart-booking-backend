package com.example.smart_booking_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadResponse {
    private String url;
    private String fileName;
    private long size;
    private String contentType;
}