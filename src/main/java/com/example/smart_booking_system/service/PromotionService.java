package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.promotion.PromotionRequestDTO;
import com.example.smart_booking_system.dto.PromotionResponseDTO;
import com.example.smart_booking_system.entity.Promotion;
import com.example.smart_booking_system.enums.PromotionStatus;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionService {

    private final PromotionRepository promotionRepository;

    // 1. TẠO MỚI
    public PromotionResponseDTO createPromotion(PromotionRequestDTO req) {
        if (promotionRepository.existsByCode(req.getCode())) {
            throw new BadRequestException("Mã khuyến mãi '" + req.getCode() + "' đã tồn tại.");
        }
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu.");
        }

        Promotion p = new Promotion();
        p.setCode(req.getCode().toUpperCase());
        p.setDescription(req.getDescription());

        // Set các trường quan trọng
        p.setDiscountType(req.getDiscountType()); // Loại giảm
        p.setDiscountValue(req.getDiscountValue());

        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setMinBookingAmount(req.getMinBookingAmount());
        p.setMaxDiscountAmount(req.getMaxDiscountAmount());
        p.setUsageLimit(req.getUsageLimit());
        p.setUsageCount(0);

        // Xử lý Status: Nếu Admin truyền lên thì lấy, không thì tự động tính
        if (req.getStatus() != null) {
            p.setStatus(req.getStatus());
        } else {
            p.checkAndSetStatus(); // Tự động check ngày để set ACTIVE/EXPIRED
        }

        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    // 2. CẬP NHẬT
    public PromotionResponseDTO updatePromotion(int id, PromotionRequestDTO req) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        // --- ✅ BỔ SUNG: LOGIC SỬA MÃ CODE ---
        // Kiểm tra xem mã có thay đổi không
        if (!p.getCode().equalsIgnoreCase(req.getCode())) {
            // Nếu đổi mã thì phải check trùng trong DB
            if (promotionRepository.existsByCode(req.getCode())) {
                throw new BadRequestException("Mã khuyến mãi '" + req.getCode() + "' đã tồn tại, vui lòng chọn mã khác.");
            }
            // Nếu hợp lệ thì cập nhật (Viết hoa)
            p.setCode(req.getCode().toUpperCase());
        }
        // -------------------------------------

        p.setDescription(req.getDescription());
        p.setDiscountType(req.getDiscountType());
        p.setDiscountValue(req.getDiscountValue());

        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BadRequestException("Ngày kết thúc không hợp lệ");
        }
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setMinBookingAmount(req.getMinBookingAmount());
        p.setMaxDiscountAmount(req.getMaxDiscountAmount());
        p.setUsageLimit(req.getUsageLimit());

        // Cập nhật Status
        if (req.getStatus() != null) {
            p.setStatus(req.getStatus());
        } else {
            // Nếu sửa ngày, cần check lại xem có bị EXPIRED không
            p.checkAndSetStatus();
        }

        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    // 3. XÓA (Chuyển sang trạng thái PAUSED hoặc EXPIRED tùy nghiệp vụ, ở đây mình để PAUSED)
    // Hoặc nếu muốn xóa cứng thì dùng deleteById.
    public void deletePromotion(int id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        // Soft delete: Chuyển sang tạm dừng
        p.setStatus(PromotionStatus.PAUSED);
        promotionRepository.save(p);
    }

    // 4. LẤY DANH SÁCH (Code gọn hơn nhờ Scheduler)
    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getAllGlobalPromotions() {
        // Chỉ cần lấy ra và map, việc update trạng thái đã có Scheduler lo
        return promotionRepository.findAll().stream()
                .map(PromotionResponseDTO::new)
                .collect(Collectors.toList());
    }

    // 5. CHI TIẾT
    @Transactional(readOnly = true)
    public PromotionResponseDTO getById(int id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        return new PromotionResponseDTO(p);
    }
}