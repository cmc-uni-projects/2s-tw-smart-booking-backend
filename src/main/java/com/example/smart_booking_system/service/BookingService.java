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
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final EmailService emailService;
    private final PaymentRepository paymentRepo;

    // ================================
    // CREATE BOOKING (chặt chẽ, capacity cho mọi loại)
    // ================================
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO req) {

        // --- 1) basic existence checks ---
        User user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + req.getUserId()));

        Property property = propertyRepo.findById(req.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property not found: " + req.getPropertyId()));

        boolean requiresWholeRoom =
                property.getPropertyType() == PropertyType.VILLA ||
                        property.getPropertyType() == PropertyType.HOMESTAY;

        Room room;

        // --- 2) choose room depending on property type ---
        if (requiresWholeRoom) {
            // FE must not send roomId for Villa/Homestay
            if (req.getRoomId() != null) {
                throw new RuntimeException("Do not send roomId for Villa/Homestay. Booking is always the WHOLE room.");
            }

            room = roomRepo.findByPropertyIdAndCategory(req.getPropertyId(), RoomCategory.WHOLE)
                    .orElseThrow(() -> new RuntimeException("Whole room (category=WHOLE) not found for property " + req.getPropertyId()));
        } else {
            // HOTEL / RESORT: require roomId and membership to property
            if (req.getRoomId() == null) {
                throw new RuntimeException("roomId is required for HOTEL/RESORT booking");
            }

            room = roomRepo.findById(req.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Room not found: " + req.getRoomId()));

            if (room.getPropertyId() == null || room.getPropertyId().getPropertyId() != req.getPropertyId()) {
                throw new RuntimeException("Room does not belong to the given property (roomId=" + req.getRoomId() + ", propertyId=" + req.getPropertyId() + ")");
            }
        }

        // --- 3) guestCount validation (required for all types now) ---
        if (req.getGuestCount() == null) {
            throw new RuntimeException("guestCount is required for booking");
        }
        if (req.getGuestCount() <= 0) {
            throw new RuntimeException("guestCount must be greater than 0");
        }

        Integer capacity = room.getCapacity();
        if (capacity == null) {
            throw new RuntimeException("Room capacity is not set for roomId: " + room.getRoomId());
        }
        if (req.getGuestCount() > capacity) {
            throw new RuntimeException("guestCount exceeds room capacity (guestCount=" + req.getGuestCount() + ", capacity=" + capacity + ")");
        }

        // --- 4) date validation ---
        if (req.getCheckInDate() == null || req.getCheckOutDate() == null) {
            throw new RuntimeException("Check-in and check-out dates are required");
        }
        if (!req.getCheckInDate().isBefore(req.getCheckOutDate())) {
            throw new RuntimeException("checkInDate must be before checkOutDate");
        }

        // --- 5) overlapping bookings check (only CONFIRMED) ---
        List<Booking> overlapping = bookingRepo.findConfirmedOverlappingByRoomId(
                room.getRoomId(),
                req.getCheckInDate(),
                req.getCheckOutDate()
        );
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Room is already booked in the selected dates");
        }

        // --- 6) price calculation ---
        long nights = ChronoUnit.DAYS.between(req.getCheckInDate(), req.getCheckOutDate());
        if (nights <= 0) nights = 1;

        BigDecimal pricePerNight = room.getPricePerNight();
        if (pricePerNight == null) {
            throw new RuntimeException("Room pricePerNight is not set for roomId: " + room.getRoomId());
        }
        BigDecimal total = pricePerNight.multiply(BigDecimal.valueOf(nights));

        // --- 7) create booking ---
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setProperty(property);
        booking.setRoom(room);
        booking.setCheckInDate(req.getCheckInDate());
        booking.setCheckOutDate(req.getCheckOutDate());
        booking.setGuestCount(req.getGuestCount());
        booking.setTotalPrice(total);
        booking.setPenaltyAmount(BigDecimal.ZERO);
        booking.setRefundAmount(total);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        bookingRepo.save(booking);


        try {
            emailService.sendPaymentReminderEmail(
                    user.getEmail(),
                    user.getFullName(),
                    String.valueOf(booking.getBookingId()),
                    booking.getTotalPrice().toString()
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi email nhắc thanh toán: " + e.getMessage());
        }

        return convertToDTO(booking);
    }

    // ================================
    // CANCEL BOOKING (chặt chẽ + safe)
    // ================================
    public BookingResponseDTO cancelBooking(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking already cancelled");
        }

        PropertyPolicies policies = policiesRepo.findByPropertyId(booking.getProperty().getPropertyId());
        LocalDate today = LocalDate.now();

        boolean allowFree = false;

        if (policies != null && Boolean.TRUE.equals(policies.isAllowFreeCancellation())) {
            Integer freeDays = policies.getFreeCancellationDays();
            if (freeDays == null) freeDays = 0;
            if (freeDays < 0) freeDays = 0;

            LocalDate deadline = booking.getCheckInDate().minusDays(freeDays);
            // allow free if today is on or before the deadline (inclusive)
            if (!today.isAfter(deadline)) {
                allowFree = true;
            }
        }

        BigDecimal refundAmount;

        if (allowFree) {
            booking.setPenaltyAmount(BigDecimal.ZERO);
            refundAmount = booking.getTotalPrice();
        } else {
            BigDecimal penalty = booking.getTotalPrice().multiply(BigDecimal.valueOf(0.20));
            booking.setPenaltyAmount(penalty);
            refundAmount = booking.getTotalPrice().subtract(penalty);
        }

        booking.setRefundAmount(refundAmount);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        // --- 2. Cập nhật thông tin hoàn tiền vào Payment ---
        paymentRepo.findByBooking_BookingId(bookingId).ifPresent(payment -> {
            payment.setRefundedAmount(refundAmount);

            String oldNote = payment.getNote() != null ? payment.getNote() : "";
            payment.setNote(oldNote + " | Đã hoàn " + refundAmount + " VND ngày " + LocalDateTime.now());

            paymentRepo.save(payment);
        });

        return convertToDTO(booking);
    }

    // ================================
    // GET APIs
    // ================================
    public BookingResponseDTO getBookingById(int bookingId) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        return convertToDTO(b);
    }

    public List<BookingResponseDTO> getBookingsByUserId(String userId) {
        return bookingRepo.findByUserUserId(userId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getBookingsByPropertyId(int propertyId) {
        return bookingRepo.findByPropertyPropertyId(propertyId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepo.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // ================================
    // helper convert
    // ================================
    private BookingResponseDTO convertToDTO(Booking b) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setBookingId(b.getBookingId());
        dto.setPropertyId(b.getProperty().getPropertyId());
        dto.setRoomId(b.getRoom() != null ? b.getRoom().getRoomId() : null);
        dto.setCheckInDate(b.getCheckInDate());
        dto.setCheckOutDate(b.getCheckOutDate());
        dto.setGuestCount(b.getGuestCount());
        dto.setTotalPrice(b.getTotalPrice());
        dto.setPenaltyAmount(b.getPenaltyAmount());
        dto.setRefundAmount(b.getRefundAmount());
        dto.setStatus(b.getStatus());
        return dto;
    }
}
