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

        p.setDiscountType(req.getDiscountType());
        p.setDiscountValue(req.getDiscountValue());

        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setMinBookingAmount(req.getMinBookingAmount());
        p.setMaxDiscountAmount(req.getMaxDiscountAmount());
        p.setUsageLimit(req.getUsageLimit());
        p.setUsageCount(0);

        if (req.getStatus() != null) {
            p.setStatus(req.getStatus());
        } else {
            p.checkAndSetStatus();
        }

        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    // 2. CẬP NHẬT
    public PromotionResponseDTO updatePromotion(int id, PromotionRequestDTO req) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));


        if (!p.getCode().equalsIgnoreCase(req.getCode())) {
            if (promotionRepository.existsByCode(req.getCode())) {
                throw new BadRequestException("Mã khuyến mãi '" + req.getCode() + "' đã tồn tại, vui lòng chọn mã khác.");
            }
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

        if (req.getStatus() != null) {
            p.setStatus(req.getStatus());
        } else {
            p.checkAndSetStatus();
        }

        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    // 3. XÓA
    public void deletePromotion(int id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));


        promotionRepository.delete(p);
    }


    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getAllGlobalPromotions() {
        return promotionRepository.findAll().stream()
                .map(PromotionResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PromotionResponseDTO getById(int id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        return new PromotionResponseDTO(p);
    }
}