package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.RoomImageResponseDTO;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.entity.RoomImage;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.repository.RoomImageRepository;
import com.example.smart_booking_system.repository.RoomRepository;
import com.example.smart_booking_system.service.impl.FileStorageServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.transaction.Transactional;
import com.example.smart_booking_system.exception.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomImageService {

    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final FileStorageServiceImpl fileStorageService;

    @Transactional
    public void setCoverImage(int roomId, int imageId) {
        // 1. Kiểm tra ảnh tồn tại
        RoomImage image = roomImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        // 2. Kiểm tra ảnh có thuộc phòng này không
        if (image.getRoom().getRoomId() != roomId) {
            throw new BadRequestException("Image does not belong to this room");
        }

        // 3. Reset các ảnh khác
        roomImageRepository.resetCoverImageByRoomId(roomId);

        // 4. Set ảnh này làm bìa
        image.setCover(true);
        roomImageRepository.save(image);
    }

    public RoomImageService(RoomRepository roomRepository,
                            RoomImageRepository roomImageRepository,
                            FileStorageServiceImpl fileStorageService) {
        this.roomRepository = roomRepository;
        this.roomImageRepository = roomImageRepository;
        this.fileStorageService = fileStorageService;
    }

    // ================== UPLOAD MULTIPLE ================== //
    public List<RoomImageResponseDTO> uploadMultipleRoomImages(int roomId, List<MultipartFile> files) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BadRequestException("Room not found"));

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("No files uploaded");
        }

        List<RoomImageResponseDTO> result = new ArrayList<>();

        for (MultipartFile file : files) {

            String savedPath = fileStorageService.storeImageFile(file, "room");

            RoomImage ri = new RoomImage();
            ri.setRoom(room);
            ri.setImageUrl(savedPath);
            ri.setActive(true);

            RoomImage saved = roomImageRepository.save(ri);

            result.add(new RoomImageResponseDTO(
                    saved.getRoomImageId(),
                    roomId,
                    saved.getImageUrl(),
                    saved.isActive(),
                    saved.isCover()
            ));
        }

        return result;
    }

    // ================== GET LIST ================== //
    public List<RoomImageResponseDTO> getImagesByRoomId(int roomId) {

        List<RoomImage> list =
                roomImageRepository.findActiveImagesByRoomId(roomId);

        return list.stream()
                .map(img -> new RoomImageResponseDTO(
                        img.getRoomImageId(),
                        img.getRoom().getRoomId(),
                        img.getImageUrl(),
                        img.isActive(),
                        img.isCover()
                ))
                .collect(Collectors.toList());
    }

    // ================== DELETE ================== //
    public void deleteRoomImage(int roomId, int imageId) {

        RoomImage img = roomImageRepository.findById(imageId)
                .orElseThrow(() -> new BadRequestException("Image not found"));

        // kiểm tra thuộc room
        if (img.getRoom().getRoomId() != roomId) {
            throw new BadRequestException("This image does not belong to this room");
        }

        // xoá file local
        fileStorageService.deleteFile(img.getImageUrl());

        // soft delete
        img.setActive(false);
        roomImageRepository.save(img);
    }
}
