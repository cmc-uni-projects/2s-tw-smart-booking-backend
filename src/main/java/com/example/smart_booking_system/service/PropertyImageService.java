package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.PropertyImageResponseDTO;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.PropertyImage;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.PropertyImageRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class PropertyImageService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final FileStorageService fileStorageService;

    public PropertyImageService(PropertyRepository propertyRepository,
                                PropertyImageRepository propertyImageRepository,
                                FileStorageService fileStorageService) {
        this.propertyRepository = propertyRepository;
        this.propertyImageRepository = propertyImageRepository;
        this.fileStorageService = fileStorageService;
    }

    // =============================
    // SET COVER
    // =============================
    public void setCoverImage(int propertyId, int imageId) {
        PropertyImage img = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("error.image.notFound"));

        if (img.getProperty().getPropertyId() != propertyId) {
            throw new BadRequestException("error.image.wrongOwner");
        }

        propertyImageRepository.resetCoverImageByPropertyId(propertyId);

        img.setCover(true);
        propertyImageRepository.save(img);
    }

    // =============================
    // UPLOAD MULTIPLE
    // =============================
    public List<PropertyImageResponseDTO> uploadMultiplePropertyImages(int propertyId,
                                                                       List<MultipartFile> files) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("error.property.notFound"));

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("error.file.noneUploaded");
        }

        return files.stream().map(file -> {

            String key = fileStorageService.storeImageFile(file, "properties/" + propertyId);

            PropertyImage pi = new PropertyImage();
            pi.setProperty(property);
            pi.setImageUrl(key);
            pi.setActive(true);

            PropertyImage saved = propertyImageRepository.save(pi);

            return new PropertyImageResponseDTO(
                    saved.getPropertyImageId(),
                    propertyId,
                    fileStorageService.generateSignedUrl(saved.getImageUrl()), // 🔥 signed URL
                    saved.isActive(),
                    saved.isCover()
            );

        }).toList();
    }

    // =============================
    // GET LIST IMAGES (SIGNED URL)
    // =============================
    public List<PropertyImageResponseDTO> getImagesByPropertyId(int propertyId) {

        List<PropertyImage> list = propertyImageRepository.findActiveImagesByPropertyId(propertyId);

        return list.stream().map(img ->
                new PropertyImageResponseDTO(
                        img.getPropertyImageId(),
                        propertyId,
                        fileStorageService.generateSignedUrl(img.getImageUrl()), // 🔥 signed URL
                        img.isActive(),
                        img.isCover()
                )
        ).toList();
    }

    // =============================
    // DELETE FILE
    // =============================
    public void deletePropertyImage(int propertyId, int imageId) {

        PropertyImage img = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("error.image.notFound"));

        if (img.getProperty().getPropertyId() != propertyId) {
            throw new BadRequestException("error.image.wrongOwner");
        }

        // Delete from cloud
        fileStorageService.deleteFile(img.getImageUrl());

        img.setActive(false);
        propertyImageRepository.save(img);
    }
}
