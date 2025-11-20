package com.example.smart_booking_system.mapper;

import com.example.smart_booking_system.dto.BookingSimpleDTO;
import com.example.smart_booking_system.entity.Booking;

public class BookingMapper {

    public static BookingSimpleDTO toSimpleDTO(Booking b) {
        BookingSimpleDTO dto = new BookingSimpleDTO();
        dto.setBookingId(b.getBookingId());
        dto.setRoomId(b.getRoom().getRoomId());
        dto.setPropertyId(b.getProperty().getPropertyId());
        dto.setUserId(b.getUser().getUserId());
        dto.setCheckInDate(b.getCheckInDate().toString());
        dto.setCheckOutDate(b.getCheckOutDate().toString());
        dto.setStatus(b.getStatus().name());
        return dto;
    }
}
