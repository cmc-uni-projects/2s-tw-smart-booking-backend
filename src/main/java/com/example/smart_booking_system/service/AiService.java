package com.example.smart_booking_system.service;

import com.example.smart_booking_system.config.GroqClient;
import com.example.smart_booking_system.dto.BookingSimpleDTO;
import com.example.smart_booking_system.dto.PropertyAmenityResponseDTO;
import com.example.smart_booking_system.dto.RoomSimpleDTO;
import com.example.smart_booking_system.dto.response.property.PropertyDetailDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.BookingStatus;
import com.example.smart_booking_system.mapper.BookingMapper;
import com.example.smart_booking_system.mapper.RoomMapper;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import com.example.smart_booking_system.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {

    private final GroqClient groqClient;
    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final PropertyService propertyService; // ✅ Inject PropertyService

    // ✅ HÀM GỘP: CHAT WITH AI
    public String chatWithAI(String userQuestion) {
        // 1. Lấy dữ liệu Top 40 kèm Room (đã bao gồm thông tin chi tiết phòng)
        List<PropertyDetailDTO> topProperties = propertyService.getTop40ForAI();

        // 2. Build Context String (Tự nhiên, KHÔNG ID)
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Danh sách các cơ sở lưu trú nổi bật:\n");

        for (PropertyDetailDTO p : topProperties) {

            // Format tiện nghi (Lấy 5 cái đầu cho gọn)
            String amenities = (p.getAmenities() != null && !p.getAmenities().isEmpty())
                    ? p.getAmenities().stream()
                    .map(PropertyAmenityResponseDTO::getAmenityName)
                    .limit(5)
                    .collect(Collectors.joining(", "))
                    : "Cơ bản";

            // ✅ Format thông tin PHÒNG (Tên phòng, Giá, Sức chứa)
            String roomsInfo = "Đang cập nhật phòng";
            if (p.getRooms() != null && !p.getRooms().isEmpty()) {
                roomsInfo = p.getRooms().stream()
                        .map(r -> String.format("%s (Giá: %s VND, Tối đa %d người)",
                                r.getRoomName(),
                                r.getPricePerNight(),
                                r.getCapacity()))
                        .collect(Collectors.joining("; "));
            }

            // ✅ Format dòng dữ liệu: BỎ ID, tập trung vào trải nghiệm
            contextBuilder.append(String.format(
                    "- %s (%s) tại %s. Đánh giá: %.1f/5. Tiện nghi: %s. Các loại phòng: %s.\n",
                    p.getPropertyName(),      // Tên
                    p.getPropertyType(),      // Loại (Hotel/Villa)
                    p.getCity(),              // Thành phố
                    p.getRating(),            // Rating
                    amenities,                // Tiện nghi
                    roomsInfo                 // Chi tiết phòng & giá
            ));
        }

        // 3. Tạo Prompt System (Hướng dẫn AI trả lời tự nhiên)
        String systemPrompt = """
            Bạn là nhân viên tư vấn du lịch thân thiện của TravelMate.
            Dưới đây là danh sách các khách sạn và phòng hiện có.
            
            Yêu cầu:
            1. Trả lời câu hỏi của khách hàng một cách tự nhiên, như người thật đang chat.
            2. KHÔNG nhắc đến ID của khách sạn trong câu trả lời. Chỉ dùng Tên khách sạn.
            3. Dựa vào thông tin 'Các loại phòng' (Giá, Sức chứa) để tư vấn chính xác cho nhóm khách (ví dụ: khách đi 4 người thì gợi ý phòng Family hoặc 2 phòng Double).
            4. Nếu khách hỏi gợi ý, hãy đưa ra 3 lựa chọn tốt nhất kèm lý do ngắn gọn.
            5. Ngôn ngữ: Tiếng Việt.
            
            Dữ liệu hệ thống:
            """ + contextBuilder.toString();

        // 4. Gọi API
        return groqClient.askGroq(systemPrompt + "\n\nKhách hàng: " + userQuestion);
    }

    // ... Các hàm book, available cũ giữ nguyên ...
    public List<RoomSimpleDTO> getAvailableRooms(String city, LocalDate checkIn, LocalDate checkOut) {
        List<Property> properties = (city == null || city.trim().isEmpty())
                ? propertyRepository.findAll().stream().filter(Property::isActive).toList()
                : propertyRepository.findAll().stream().filter(Property::isActive).filter(p -> p.getCity().equalsIgnoreCase(city)).toList();

        List<Room> rooms = roomRepository.findAll().stream()
                .filter(Room::isActive)
                .filter(r -> properties.stream().anyMatch(p -> p.getPropertyId() == r.getPropertyId().getPropertyId()))
                .toList();

        List<Room> available = rooms.stream()
                .filter(r -> bookingRepository.findAll().stream().noneMatch(b ->
                        b.getRoom().getRoomId() == r.getRoomId() &&
                                b.getCheckInDate().isBefore(checkOut) &&
                                b.getCheckOutDate().isAfter(checkIn)
                ))
                .toList();

        return available.stream().map(RoomMapper::toSimpleDTO).toList();
    }

    public BookingSimpleDTO bookRoom(Integer roomId, LocalDate checkIn, LocalDate checkOut, User user) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room not found"));
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setProperty(room.getPropertyId());
        booking.setUser(user);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setTotalPrice(BigDecimal.ZERO);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setGuestCount(1);
        Booking saved = bookingRepository.save(booking);
        return BookingMapper.toSimpleDTO(saved);
    }
}