package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.PropertyImageResponseDTO;
import com.example.smart_booking_system.service.PropertyImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/propertyImage")
@RequiredArgsConstructor
public class PropertyImageController {

    private final PropertyImageService propertyImageService;


        @PostMapping("/upload-multiple/{propertyId}")
    public ResponseEntity<?> uploadMultiple(
            @PathVariable int propertyId,
            @RequestParam("file") List<MultipartFile> files) {

        return ResponseEntity.ok(
                propertyImageService.uploadMultiplePropertyImages(propertyId, files)
        );
    }


    @GetMapping("/{propertyId}")
    public ResponseEntity<?> getByProperty(@PathVariable int propertyId) {

        return ResponseEntity.ok(
                propertyImageService.getImagesByPropertyId(propertyId)
        );
    }

    @DeleteMapping("/delete/{propertyId}/{imageId}")
    public ResponseEntity<?> delete(
            @PathVariable int propertyId,
            @PathVariable int imageId) {

        propertyImageService.deletePropertyImage(propertyId, imageId);
        return ResponseEntity.ok("Deleted");
    }

}
