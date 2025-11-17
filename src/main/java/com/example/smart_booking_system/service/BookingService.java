package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.BookingResponseDTO;
import com.example.smart_booking_system.dto.request.BookingRequestDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.BookingStatus;
import com.example.smart_booking_system.enums.PropertyType;
import com.example.smart_booking_system.enums.RoomCategory;
import com.example.smart_booking_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepo;
    private final PropertyRepository propertyRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final PropertyPoliciesRepository policiesRepo;

    // ---------------------------
    // CREATE (như trước)
    // ---------------------------
    public BookingResponseDTO createBooking(BookingRequestDTO req) {
        // (copy existing createBooking implementation)
        User user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Property property = propertyRepo.findById(req.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        boolean requiresWholeRoom = property.getPropertyType() == PropertyType.VILLA
                || property.getPropertyType() == PropertyType.HOMESTAY;

        Room room;
        if (requiresWholeRoom) {
            room = roomRepo.findByPropertyIdAndCategory(req.getPropertyId(), RoomCategory.WHOLE)
                    .orElseThrow(() -> new RuntimeException("Whole room for property not found"));
        } else {
            if (req.getRoomId() == null) {
                throw new RuntimeException("roomId is required for HOTEL/RESORT booking");
            }
            room = roomRepo.findById(req.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Room not found"));
            if (room.getPropertyId() == null || room.getPropertyId().getPropertyId() != req.getPropertyId()) {
                throw new RuntimeException("Room does not belong to the given property");
            }
        }

        if (req.getCheckInDate() == null || req.getCheckOutDate() == null) {
            throw new RuntimeException("Check-in and check-out dates are required");
        }
        if (!req.getCheckInDate().isBefore(req.getCheckOutDate())) {
            throw new RuntimeException("checkInDate must be before checkOutDate");
        }

        List<Booking> overlapping = bookingRepo.findConfirmedOverlappingByRoomId(
                room.getRoomId(), req.getCheckInDate(), req.getCheckOutDate());

        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Room is already booked in the selected dates");
        }

        long nights = ChronoUnit.DAYS.between(req.getCheckInDate(), req.getCheckOutDate());
        if (nights <= 0) nights = 1;

        BigDecimal pricePerNight = room.getPricePerNight();
        if (pricePerNight == null) {
            throw new RuntimeException("Room price is not set");
        }

        BigDecimal total = pricePerNight.multiply(BigDecimal.valueOf(nights));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setProperty(property);
        booking.setRoom(room);
        booking.setCheckInDate(req.getCheckInDate());
        booking.setCheckOutDate(req.getCheckOutDate());
        booking.setTotalPrice(total);
        booking.setPenaltyAmount(BigDecimal.ZERO);
        booking.setRefundAmount(total);
        booking.setStatus(BookingStatus.CONFIRMED);

        bookingRepo.save(booking);

        return convertToDTO(booking);
    }

    // ---------------------------
    // CANCEL (như trước)
    // ---------------------------
    public BookingResponseDTO cancelBooking(int bookingId) {

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking already cancelled");
        }

        PropertyPolicies policies = null;
        try {
            policies = policiesRepo.findByPropertyId(booking.getProperty().getPropertyId());
        } catch (Exception ex) {
            policies = null;
        }

        LocalDate today = LocalDate.now();

        boolean allowFree = false;
        if (policies != null && Boolean.TRUE.equals(policies.isAllowFreeCancellation())) {
            Integer freeDays = policies.getFreeCancellationDays();
            if (freeDays == null) freeDays = 0;
            LocalDate deadline = booking.getCheckInDate().minusDays(freeDays);
            if (!today.isAfter(deadline)) {
                allowFree = true;
            }
        }

        if (allowFree) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setPenaltyAmount(BigDecimal.ZERO);
            booking.setRefundAmount(booking.getTotalPrice());
            bookingRepo.save(booking);
            return convertToDTO(booking);
        }

        BigDecimal penalty = booking.getTotalPrice().multiply(BigDecimal.valueOf(0.20));
        BigDecimal refund = booking.getTotalPrice().subtract(penalty);

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setPenaltyAmount(penalty);
        booking.setRefundAmount(refund);

        bookingRepo.save(booking);
        return convertToDTO(booking);
    }

    // ---------------------------
    // READ APIs (mới)
    // ---------------------------

    // 1) Get booking by id
    public BookingResponseDTO getBookingById(int bookingId) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        return convertToDTO(b);
    }

    // 2) Get bookings by userId (all bookings for a user)
    public List<BookingResponseDTO> getBookingsByUserId(String userId) {
        List<Booking> list = bookingRepo.findByUserUserId(userId);
        return list.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // 3) Get bookings by propertyId (all bookings for a property)
    public List<BookingResponseDTO> getBookingsByPropertyId(int propertyId) {
        List<Booking> list = bookingRepo.findByPropertyPropertyId(propertyId);
        return list.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // 4) Get all bookings (optionally you can add pagination later)
    public List<BookingResponseDTO> getAllBookings() {
        List<Booking> list = bookingRepo.findAll();
        return list.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // ---------------------------
    // helper convert
    // ---------------------------
    private BookingResponseDTO convertToDTO(Booking b) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setBookingId(b.getBookingId());
        dto.setPropertyId(b.getProperty().getPropertyId());
        dto.setRoomId(b.getRoom() != null ? b.getRoom().getRoomId() : null);
        dto.setCheckInDate(b.getCheckInDate());
        dto.setCheckOutDate(b.getCheckOutDate());
        dto.setTotalPrice(b.getTotalPrice());
        dto.setPenaltyAmount(b.getPenaltyAmount());
        dto.setRefundAmount(b.getRefundAmount());
        dto.setStatus(b.getStatus());
        return dto;
    }
}
