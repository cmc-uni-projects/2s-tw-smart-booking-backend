package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.RoomResponseDTO; // ✅ Nhớ import DTO này
import com.example.smart_booking_system.dto.request.room.RoomRequestDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.AmenityType;
import com.example.smart_booking_system.enums.RoomStatus;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.*;
import com.example.smart_booking_system.service.FileStorageService;
import com.example.smart_booking_system.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final PropertyRepository propertyRepository;
    private final AmenityRepository amenityRepository;
    private final RoomAmenityRepository roomAmenityRepository;
    private final RoomImageRepository roomImageRepository;
    private final FileStorageService fileStorageService;


    // HÀM KIỂM TRA TÊN PHÒNG ĐÃ TỒN TẠI CHƯA
    @Override
    public boolean checkRoomNameExists(int propertyId, String roomName, int excludeRoomId) {
        // excludeRoomId = 0 nếu là tạo mới (vì ID tự tăng bắt đầu từ 1)
        return roomRepository.existsByPropertyIdAndRoomNameAndIdNot(propertyId, roomName, excludeRoomId);
    }

    //  SỬA LỖI CÚ PHÁP & LOGIC: Trả về List<RoomResponseDTO>
    @Override
    @Transactional(readOnly = true)
    public List<RoomResponseDTO> getRoomsByPropertyId(int propertyId) {
        // ✅ SỬA: Gọi hàm AndActiveTrue để lọc bỏ phòng đã xóa
        List<Room> rooms = roomRepository.findByPropertyId_PropertyIdAndIsActiveTrue(propertyId);

        // Map từng Room sang DTO
        return rooms.stream()
                .map(this::mapToRoomDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RoomResponseDTO addRoom(RoomRequestDTO dto, List<MultipartFile> images) {
        // 1. Check Property
        Property property = propertyRepository.findById(dto.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        // 2. Map DTO -> Entity Room
        Room room = new Room();
        room.setPropertyId(property);
        room.setRoomName(dto.getRoomName());
        room.setRoomCategory(dto.getRoomCategory());
        room.setPricePerNight(dto.getPricePerNight());
        room.setCapacity(dto.getCapacity());
        room.setDescription(dto.getDescription());
        room.setRoomStatus(RoomStatus.AVAILABLE);
        room.setActive(true);

        Room savedRoom = roomRepository.save(room);

        // 3. Save Amenities (Tiện nghi phòng)
        if (dto.getAmenities() != null) {
            for (String amenityKey : dto.getAmenities()) {
                // Tìm amenity theo key và loại ROOM
                amenityRepository.findByAmenityNameAndAmenityType(amenityKey, AmenityType.ROOM)
                        .ifPresent(amenity -> {
                            RoomAmenity ra = new RoomAmenity();
                            ra.setRoom(savedRoom);     // ✅ Đã sửa setRoom
                            ra.setAmenity(amenity);    // ✅ Đã sửa setAmenity
                            ra.setActive(true);
                            roomAmenityRepository.save(ra);
                        });
            }
        }

        // 4. Save Images
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                String path = fileStorageService.storeImageFile(file, "rooms");
                RoomImage roomImage = new RoomImage();
                roomImage.setRoom(savedRoom); // ✅ Đã sửa setRoom
                roomImage.setImageUrl(path);
                roomImageRepository.save(roomImage);
            }
        }

        return mapToRoomDTO(savedRoom);
    }

    @Override
    public RoomResponseDTO updateRoom(int roomId, RoomRequestDTO dto, List<MultipartFile> newImages) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        // Update basic info
        room.setRoomName(dto.getRoomName());
        room.setRoomCategory(dto.getRoomCategory());
        room.setPricePerNight(dto.getPricePerNight());
        room.setCapacity(dto.getCapacity());
        room.setDescription(dto.getDescription());

        Room savedRoom = roomRepository.save(room);

        // Xử lý ảnh mới thêm vào
        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile file : newImages) {
                String path = fileStorageService.storeImageFile(file, "rooms");
                RoomImage roomImage = new RoomImage();
                roomImage.setRoom(room);
                roomImage.setImageUrl(path);
                roomImageRepository.save(roomImage);
            }
        }

        return mapToRoomDTO(savedRoom);
    }

    @Override
    public void deleteRoom(int roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        room.setActive(false); // Soft delete
        roomRepository.save(room);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponseDTO getRoomById(int roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        // Hàm này đã bao gồm logic lấy images và amenities
        return mapToRoomDTO(room);
    }

    // ✅ HÀM HELPER QUAN TRỌNG: Chuyển đổi Entity -> DTO
    private RoomResponseDTO mapToRoomDTO(Room room) {
        RoomResponseDTO dto = new RoomResponseDTO();
        dto.setRoomId(room.getRoomId());
        dto.setPropertyId(room.getPropertyId().getPropertyId());
        dto.setRoomName(room.getRoomName());
        dto.setRoomCategory(room.getRoomCategory());
        dto.setPricePerNight(room.getPricePerNight());
        dto.setCapacity(room.getCapacity());
        dto.setDescription(room.getDescription());
        dto.setRoomStatus(room.getRoomStatus());
        dto.setActive(room.isActive());

        // 1. Lấy danh sách ảnh
        // Đảm bảo RoomImageRepository có hàm findByRoom_RoomId
        List<String> imageUrls = roomImageRepository.findByRoom_RoomId(room.getRoomId())
                .stream()
                .map(RoomImage::getImageUrl)
                .collect(Collectors.toList());
        dto.setImages(imageUrls);

        // 2. Lấy danh sách tiện nghi
        // Đảm bảo RoomAmenityRepository có hàm findByRoom_RoomId
        List<String> amenityNames = roomAmenityRepository.findByRoom_RoomId(room.getRoomId())
                .stream()
                .filter(RoomAmenity::isActive)
                .map(ra -> ra.getAmenity().getAmenityName())
                .collect(Collectors.toList());
        dto.setAmenities(amenityNames);

        return dto;
    }
}