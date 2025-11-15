package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.RoomAmenityResponseDTO;
import com.example.smart_booking_system.dto.request.UpdateRoomAmenityRequest;
import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.entity.RoomAmenity;
import com.example.smart_booking_system.enums.AmenityType;
import com.example.smart_booking_system.repository.AmenityRepository;
import com.example.smart_booking_system.repository.RoomAmenityRepository;
import com.example.smart_booking_system.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomAmenityService {

    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;
    private final RoomAmenityRepository roomAmenityRepository;

    private RoomAmenityResponseDTO toDTO(RoomAmenity ra) {
        RoomAmenityResponseDTO dto = new RoomAmenityResponseDTO();
        dto.setRoomAmenityId(ra.getRoomAmenityId());
        dto.setRoomId(ra.getRoomId().getRoomId());
        dto.setAmenityId(ra.getAmenityId().getAmenityId());
        dto.setAmenityName(ra.getAmenityId().getAmenityName());
        return dto;
    }

    // ------------------ ADD ------------------
    public List<RoomAmenityResponseDTO> addMultipleAmenities(int roomId, List<Integer> amenityIds) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!room.isActive())
            throw new IllegalArgumentException("Room is inactive");

        List<Integer> invalidAmenityIds = new ArrayList<>();

        // 1. VALIDATION PHASE
        for (int amenityId : amenityIds) {

            Amenity amenity = amenityRepository.findById(amenityId).orElse(null);

            if (amenity == null ||
                    !amenity.isActive() ||
                    amenity.getAmenityType() != AmenityType.ROOM) {

                invalidAmenityIds.add(amenityId);
            }
        }

        // 2. Nếu có lỗi → trả về toàn bộ lỗi
        if (!invalidAmenityIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid amenityIds: " + invalidAmenityIds
            );
        }

        // 3. ADD PHASE — tất cả đã hợp lệ
        List<RoomAmenityResponseDTO> result = new ArrayList<>();

        for (int amenityId : amenityIds) {
            if (roomAmenityRepository.existsByRoomAndAmenity(roomId, amenityId))
                continue;

            Amenity amenity = amenityRepository.findById(amenityId).get();

            RoomAmenity ra = new RoomAmenity();
            ra.setRoomId(room);
            ra.setAmenityId(amenity);
            ra.setActive(true);

            result.add(toDTO(roomAmenityRepository.save(ra)));
        }

        return result;
    }



    // ------------------ READ ------------------
    public List<RoomAmenityResponseDTO> getRoomAmenities(int roomId) {
        return roomAmenityRepository.findByRoomId(roomId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }


    //update
    public RoomAmenityResponseDTO updateRoomAmenityByAmenityId(int roomId, int oldAmenityId, int newAmenityId) {

        // 1. Tìm RoomAmenity cũ (amenityId đang gán)
        RoomAmenity existing = roomAmenityRepository.findByRoomIdAndAmenityId(roomId, oldAmenityId);

        if (existing == null) {
            throw new IllegalArgumentException("This amenity does not belong to this room");
        }

        // 2. Lấy amenity mới
        Amenity newAmenity = amenityRepository.findById(newAmenityId)
                .orElseThrow(() -> new IllegalArgumentException("New amenity not found"));

        // 3. Kiểm tra type phải đúng ROOM
        if (newAmenity.getAmenityType() != AmenityType.ROOM) {
            throw new IllegalArgumentException("Amenity type must be ROOM");
        }

        // 4. Kiểm tra room đã có amenity mới chưa
        boolean alreadyAssigned =
                roomAmenityRepository.findByRoomIdAndAmenityId(roomId, newAmenityId) != null;

        if (alreadyAssigned) {
            throw new IllegalArgumentException("This room already has this amenity");
        }

        // 5. Update
        existing.setAmenityId(newAmenity);
        RoomAmenity saved = roomAmenityRepository.save(existing);

        // 6. TRẢ DTO → KHÔNG BAO GIỜ LỖI BYTEBUDDY
        return new RoomAmenityResponseDTO(
                saved.getRoomAmenityId(),
                saved.getRoomId().getRoomId(),
                saved.getAmenityId().getAmenityId(),
                saved.getAmenityId().getAmenityName()
        );
    }



    // ------------------ DELETE ------------------
    @Transactional
    public RoomAmenityResponseDTO deleteRoomAmenity(int roomId, int amenityId) {

        // 1. Kiểm tra amenity có thuộc room không
        RoomAmenity existing = roomAmenityRepository
                .findByRoomIdAndAmenityId(roomId, amenityId);

        if (existing == null) {
            throw new IllegalArgumentException("This amenity does not belong to this room");
        }

        // 2. Soft delete
        roomAmenityRepository.softDelete(existing.getRoomAmenityId());

        // 3. Trả về DTO (để FE cập nhật real-time)
        return new RoomAmenityResponseDTO(
                existing.getRoomAmenityId(),
                roomId,
                amenityId,
                existing.getAmenityId().getAmenityName()
        );
    }

}
