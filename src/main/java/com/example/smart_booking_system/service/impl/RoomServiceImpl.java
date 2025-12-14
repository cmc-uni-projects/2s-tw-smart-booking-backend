package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.RoomResponseDTO;
import com.example.smart_booking_system.dto.request.room.RoomRequestDTO;
import com.example.smart_booking_system.dto.response.PriceForecastDTO;
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

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
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

    @Override
    public boolean checkRoomNameExists(int propertyId, String roomName, int excludeRoomId) {
        return roomRepository.existsByPropertyIdAndRoomNameAndIdNot(propertyId, roomName, excludeRoomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponseDTO> getRoomsByPropertyId(int propertyId) {
        List<Room> rooms = roomRepository.findByPropertyId_PropertyIdAndIsActiveTrue(propertyId);
        return rooms.stream().map(this::mapToRoomDTO).collect(Collectors.toList());
    }

    @Override
    public RoomResponseDTO addRoom(RoomRequestDTO dto, List<MultipartFile> images) {
        Property property = propertyRepository.findById(dto.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        Room room = new Room();
        room.setPropertyId(property);
        room.setRoomName(dto.getRoomName());
        room.setRoomCategory(dto.getRoomCategory());
        room.setPricePerNight(dto.getPricePerNight());

        // Nếu không nhập weekendPrice, lấy bằng giá thường
        if (dto.getWeekendPrice() != null && dto.getWeekendPrice().compareTo(BigDecimal.ZERO) > 0) {
            room.setWeekendPrice(dto.getWeekendPrice());
        } else {
            room.setWeekendPrice(dto.getPricePerNight());
        }

        room.setCapacity(dto.getCapacity());
        room.setDescription(dto.getDescription());
        room.setRoomStatus(RoomStatus.AVAILABLE);
        room.setActive(true);

        Room savedRoom = roomRepository.save(room);

        // Lưu Amenities
        if (dto.getAmenities() != null) {
            for (String amenityKey : dto.getAmenities()) {
                amenityRepository.findByAmenityNameAndAmenityType(amenityKey, AmenityType.ROOM)
                        .ifPresent(amenity -> {
                            RoomAmenity ra = new RoomAmenity();
                            ra.setRoom(savedRoom);
                            ra.setAmenity(amenity);
                            ra.setActive(true);
                            roomAmenityRepository.save(ra);
                        });
            }
        }

        // Lưu Images
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                String path = fileStorageService.storeImageFile(file, "rooms");
                RoomImage roomImage = new RoomImage();
                roomImage.setRoom(savedRoom);
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

        room.setRoomName(dto.getRoomName());
        room.setRoomCategory(dto.getRoomCategory());
        room.setPricePerNight(dto.getPricePerNight());


        if (dto.getWeekendPrice() != null && dto.getWeekendPrice().compareTo(BigDecimal.ZERO) > 0) {
            room.setWeekendPrice(dto.getWeekendPrice());
        } else {
            room.setWeekendPrice(dto.getPricePerNight());
        }

        room.setCapacity(dto.getCapacity());
        room.setDescription(dto.getDescription());

        Room savedRoom = roomRepository.save(room);

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
        room.setActive(false);
        roomRepository.save(room);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponseDTO getRoomById(int roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
        return mapToRoomDTO(room);
    }

    //  Logic lấy giá theo ngày
    @Override
    @Transactional(readOnly = true)
    public List<PriceForecastDTO> getPriceForecast(int roomId, LocalDate startDate, int days) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        List<PriceForecastDTO> forecastList = new ArrayList<>();
        LocalDate current = (startDate != null) ? startDate : LocalDate.now();

        for (int i = 0; i < days; i++) {
            LocalDate date = current.plusDays(i);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);

            // Nếu là cuối tuần thì lấy weekendPrice, ngược lại lấy pricePerNight
            BigDecimal price = isWeekend ? room.getWeekendPrice() : room.getPricePerNight();

            forecastList.add(new PriceForecastDTO(date, dayOfWeek.name(), price, isWeekend));
        }
        return forecastList;
    }

    private RoomResponseDTO mapToRoomDTO(Room room) {
        RoomResponseDTO dto = new RoomResponseDTO();
        dto.setRoomId(room.getRoomId());
        dto.setPropertyId(room.getPropertyId().getPropertyId());
        dto.setRoomName(room.getRoomName());
        dto.setRoomCategory(room.getRoomCategory());
        dto.setPricePerNight(room.getPricePerNight());

        // Map weekendPrice
        dto.setWeekendPrice(room.getWeekendPrice());

        dto.setCapacity(room.getCapacity());
        dto.setDescription(room.getDescription());
        dto.setRoomStatus(room.getRoomStatus());
        dto.setActive(room.isActive());

        List<String> imageUrls = roomImageRepository.findByRoom_RoomId(room.getRoomId())
                .stream().map(RoomImage::getImageUrl).collect(Collectors.toList());
        dto.setImages(imageUrls);

        List<String> amenityNames = roomAmenityRepository.findByRoom_RoomId(room.getRoomId())
                .stream().filter(RoomAmenity::isActive)
                .map(ra -> ra.getAmenity().getAmenityName()).collect(Collectors.toList());
        dto.setAmenities(amenityNames);

        return dto;
    }
}