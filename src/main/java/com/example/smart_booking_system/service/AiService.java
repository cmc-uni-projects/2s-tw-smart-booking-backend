package com.example.smart_booking_system.service;

import com.example.smart_booking_system.config.GroqClient;
import com.example.smart_booking_system.dto.PropertySimpleDTO;
import com.example.smart_booking_system.dto.RoomSimpleDTO;
import com.example.smart_booking_system.dto.BookingSimpleDTO;
import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.BookingStatus;
import com.example.smart_booking_system.mapper.BookingMapper;
import com.example.smart_booking_system.mapper.PropertyMapper;
import com.example.smart_booking_system.mapper.RoomMapper;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import com.example.smart_booking_system.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AiService {

    private final GroqClient groqClient;
    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public AiService(
            GroqClient groqClient,
            PropertyRepository propertyRepository,
            RoomRepository roomRepository,
            BookingRepository bookingRepository
    ) {
        this.groqClient = groqClient;
        this.propertyRepository = propertyRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }


    public String askAboutProperties(String question) {

        List<Property> list = propertyRepository.findAll();

        String summary = list.stream()
                .map(p -> String.format(
                        "ID: %d | %s | City: %s | Đánh giá: %.1f | %d lượt đánh giá",
                        p.getPropertyId(),
                        p.getPropertyName(),
                        p.getCity(),
                        p.getRating(),
                        p.getReviewCount()
                ))
                .reduce("", (a, b) -> a + b + "\n");

        String prompt =
                "Dưới đây là danh sách property hiện có trong hệ thống:\n\n" +
                        summary +
                        "\nHãy trả lời câu hỏi sau dựa trên dữ liệu: \n" +
                        question;

        return groqClient.askGroq(prompt);
    }



    public List<PropertySimpleDTO> suggestProperties() {
        return propertyRepository.findFeaturedProperties()
                .stream()
                .map(PropertyMapper::toSimpleDTO)
                .toList();
    }



    public List<RoomSimpleDTO> getAvailableRooms(String city, LocalDate checkIn, LocalDate checkOut) {


        List<Property> properties =
                (city == null || city.trim().isEmpty())
                        ? propertyRepository.findAll().stream()
                        .filter(Property::isActive)
                        .toList()
                        : propertyRepository.findAll().stream()
                        .filter(Property::isActive)
                        .filter(p -> p.getCity().equalsIgnoreCase(city))
                        .toList();


        List<Room> rooms = roomRepository.findAll().stream()
                .filter(Room::isActive)
                .filter(r ->
                        properties.stream().anyMatch(
                                p -> p.getPropertyId() == r.getPropertyId().getPropertyId()
                        )
                )
                .toList();


        List<Room> available = rooms.stream()
                .filter(r -> bookingRepository.findAll().stream().noneMatch(b ->
                        b.getRoom().getRoomId() == r.getRoomId() &&
                                b.getCheckInDate().isBefore(checkOut) &&
                                b.getCheckOutDate().isAfter(checkIn)
                ))
                .toList();


        return available.stream()
                .map(RoomMapper::toSimpleDTO)
                .toList();
    }


    public BookingSimpleDTO bookRoom(Integer roomId, LocalDate checkIn, LocalDate checkOut, User user) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setProperty(room.getPropertyId());
        booking.setUser(user);

        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);

        // Tính tiền (tạm thời để 0)
        booking.setTotalPrice(BigDecimal.ZERO);

        booking.setStatus(BookingStatus.CONFIRMED);

        Booking saved = bookingRepository.save(booking);
        return BookingMapper.toSimpleDTO(saved);
    }
}
