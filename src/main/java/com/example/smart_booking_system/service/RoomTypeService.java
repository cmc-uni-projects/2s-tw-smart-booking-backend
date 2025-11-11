package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.RoomType;
import com.example.smart_booking_system.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    public RoomType addRoomType(RoomType roomType) {
        if (roomType.getRoomTypeName() ==  null || roomType.getRoomTypeName().trim().isEmpty()){
            throw new IllegalArgumentException("RoomType name can not empty");
        }

        if (roomType.getDescription() ==  null || roomType.getDescription().trim().isEmpty()){
            throw new IllegalArgumentException("RoomType description can not empty");
        }
        if (roomType.getCapacity()<0){
            throw new IllegalArgumentException("RoomType capacity can not less than 0");
        }
        if (roomType.getPolicy() ==  null || roomType.getPolicy().trim().isEmpty()){
            throw new IllegalArgumentException("RoomType policy can not empty");
        }

        roomTypeRepository.save(roomType);
        return roomType;
    }
}
