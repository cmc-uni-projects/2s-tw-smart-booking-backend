package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.PropertyImageResponseDTO;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.PropertyImage;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.repository.PropertyImageRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import com.example.smart_booking_system.service.impl.FileStorageServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.transaction.Transactional;
import com.example.smart_booking_system.exception.ResourceNotFoundException;

import java.util.List;

@Service
public class PropertyImageService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final FileStorageServiceImpl fileStorageService;

    @Transactional // Nhớ thêm Transactional vì có update DB
    public void setCoverImage(int propertyId, int imageId) {
        // 1. Kiểm tra ảnh có tồn tại và thuộc về property không
        PropertyImage image = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        if (image.getProperty().getPropertyId() != propertyId) {
            throw new BadRequestException("Image does not belong to this property");
        }

        // 2. Reset tất cả ảnh khác thành isCover = false
        propertyImageRepository.resetCoverImageByPropertyId(propertyId);

        // 3. Set ảnh này thành isCover = true
        image.setCover(true); // Lombok setter: setIsCover -> setCover
        propertyImageRepository.save(image);
    }

    public PropertyImageService(PropertyRepository propertyRepository,
                                PropertyImageRepository propertyImageRepository,
                                FileStorageServiceImpl fileStorageService) {
        this.propertyRepository = propertyRepository;
        this.propertyImageRepository = propertyImageRepository;
        this.fileStorageService = fileStorageService;
    }


    // ================== UPLOAD MULTIPLE ================== //
    public List<PropertyImageResponseDTO> uploadMultiplePropertyImages(int propertyId, List<MultipartFile> files) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new BadRequestException("Property not found"));

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("No files uploaded");
        }

        return files.stream()
                .map(file -> {
                    String savedPath = fileStorageService.storeImageFile(file, "properties");

                    PropertyImage pi = new PropertyImage();
                    pi.setProperty(property);
                    pi.setImageUrl(savedPath);
                    pi.setActive(true);
                    // Mặc định isCover là false khi upload thêm qua API này

                    PropertyImage saved = propertyImageRepository.save(pi);

                    return new PropertyImageResponseDTO(
                            saved.getPropertyImageId(),
                            propertyId,
                            saved.getImageUrl(),
                            saved.isCover(), // ✅ SỬA LỖI: Thêm trường isCover vào đây
                            saved.isActive()
                    );
                })
                .toList();
    }


    // ================== GET LIST ================== //
    public List<PropertyImageResponseDTO> getImagesByPropertyId(int propertyId) {

        List<PropertyImage> list =
                propertyImageRepository.findActiveImagesByPropertyId(propertyId);

        return list.stream()
                .map(img -> new PropertyImageResponseDTO(
                        img.getPropertyImageId(),
                        img.getProperty().getPropertyId(),
                        img.getImageUrl(),
                        img.isCover(), // ✅ SỬA LỖI: Thêm trường isCover vào đây
                        img.isActive()
                ))
                .toList();
    }

    // ================== DELETE ================== //
    public void deletePropertyImage(int propertyId, int imageId) {

        PropertyImage img = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new BadRequestException("Image not found"));

        // Kiểm tra image có thuộc property hay không
        if (img.getProperty().getPropertyId() != propertyId) {
            throw new BadRequestException("This image does not belong to this property");
        }

        // Xoá file local
        fileStorageService.deleteFile(img.getImageUrl());

        // Soft delete
        img.setActive(false);
        propertyImageRepository.save(img);
    }

}