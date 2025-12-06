package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.promotion.PromotionRequestDTO;
import com.example.smart_booking_system.dto.PromotionResponseDTO;
import com.example.smart_booking_system.entity.Promotion;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.DiscountType;
import com.example.smart_booking_system.enums.MembershipRank;
import com.example.smart_booking_system.enums.PromotionStatus;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ForbiddenException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PromotionRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import com.example.smart_booking_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final FileStorageService fileStorageService;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    // Inject AuthService để lấy user
    private final AuthService authService;

    // 1. TẠO MỚI
    public PromotionResponseDTO createPromotion(PromotionRequestDTO req) {
        // ✅ GỌI HÀM MỚI TỪ AUTH SERVICE
        User currentUser = authService.getCurrentUser();

        if (promotionRepository.existsByCode(req.getCode())) {
            throw new BadRequestException("Mã khuyến mãi '" + req.getCode() + "' đã tồn tại.");
        }

        if (req.getStartDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Ngày bắt đầu không được chọn trong quá khứ.");
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
        p.setMinMembershipRank(req.getMinMembershipRank());
        p.setUsageLimit(req.getUsageLimit());
        p.setUsageCount(0);

        if (req.getPropertyId() != null) {
            // Owner tạo mã
            Property property = propertyRepository.findById(req.getPropertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

            boolean isOwner = property.getOwner().getUserId().equals(currentUser.getUserId());
            // ✅ SỬA LỖI: Dùng hasRole thay vì getRole()
            boolean isAdmin = currentUser.hasRole("ADMIN");

            if (!isOwner && !isAdmin) {
                throw new ForbiddenException("Bạn không có quyền tạo khuyến mãi cho tài sản này.");
            }
            p.setProperty(property);
        } else {
            // Admin tạo mã toàn sàn
            // ✅ SỬA LỖI: Dùng hasRole thay vì getRole()
            if (!currentUser.hasRole("ADMIN")) {
                throw new ForbiddenException("Chỉ Admin mới được tạo mã khuyến mãi toàn sàn.");
            }
            p.setProperty(null);
        }

        if (req.getStatus() != null) {
            p.setStatus(req.getStatus());
        } else {
            p.checkAndSetStatus();
        }

        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    // ... (Các hàm update, delete, getById giữ nguyên như file trước, logic không đổi) ...
    // Nếu bạn cần code đầy đủ của các hàm dưới, hãy báo tôi gửi lại full file.

    // 2. GỢI Ý MÃ (Hàm này quan trọng, tôi viết lại để đảm bảo logic)
    public PromotionResponseDTO suggestBestPromotion(String userId, Integer propertyId, BigDecimal bookingAmount) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        BigDecimal totalSpent = bookingRepository.calculateTotalSpentByUser(userId);
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;
        int currentPoints = totalSpent.divide(BigDecimal.valueOf(1000)).intValue();
        MembershipRank userRank = BookingService.calculateRankFromPoints(currentPoints);

        List<Promotion> activePromotions;
        if (propertyId != null) {
            // Tìm cả mã toàn sàn + mã của property này
            activePromotions = promotionRepository.findPromotionsForProperty(propertyId, LocalDateTime.now());
        } else {
            // Chỉ tìm mã toàn sàn
            activePromotions = promotionRepository.findByPropertyIsNull().stream()
                    .filter(p -> p.getStatus() == PromotionStatus.ACTIVE
                            && p.getStartDate().isBefore(LocalDateTime.now())
                            && p.getEndDate().isAfter(LocalDateTime.now()))
                    .collect(Collectors.toList());
        }

        Promotion bestPromotion = activePromotions.stream()
                .filter(p -> isRankEligible(userRank, p.getMinMembershipRank()))
                .filter(p -> p.getMinBookingAmount() == null || bookingAmount.compareTo(p.getMinBookingAmount()) >= 0)
                .sorted(Comparator.comparing((Promotion p) -> calculateDiscountAmount(p, bookingAmount)).reversed())
                .findFirst()
                .orElse(null);

        if (bestPromotion == null) {
            return null;
        }

        return new PromotionResponseDTO(bestPromotion);
    }

    // --- Helper ---
    private boolean isRankEligible(MembershipRank userRank, MembershipRank requiredRank) {
        if (requiredRank == null) return true;
        return userRank.ordinal() >= requiredRank.ordinal();
    }

    private BigDecimal calculateDiscountAmount(Promotion p, BigDecimal bookingAmount) {
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (p.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discountAmount = p.getDiscountValue();
        } else if (p.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = bookingAmount.multiply(p.getDiscountValue()).divide(BigDecimal.valueOf(100));
            if (p.getMaxDiscountAmount() != null && discountAmount.compareTo(p.getMaxDiscountAmount()) > 0) {
                discountAmount = p.getMaxDiscountAmount();
            }
        }
        return discountAmount.min(bookingAmount);
    }

    // Các hàm updatePromotion, deletePromotion, toggleStatus, getPromotionsByProperty, getAllGlobalPromotions, uploadBanner, getById
    // Bạn có thể giữ nguyên nội dung hàm như câu trả lời trước, chỉ cần lưu ý import và injection đã sửa ở trên.

    // Ví dụ hàm updatePromotion rút gọn để bạn copy nếu cần:
    public PromotionResponseDTO updatePromotion(int id, PromotionRequestDTO req) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        if (p.getStatus() == PromotionStatus.DELETED) throw new BadRequestException("Không thể sửa mã đã xóa.");

        if (!p.getCode().equalsIgnoreCase(req.getCode()) && promotionRepository.existsByCode(req.getCode())) {
            throw new BadRequestException("Mã đã tồn tại.");
        }

        p.setCode(req.getCode().toUpperCase());
        p.setDescription(req.getDescription());
        p.setDiscountType(req.getDiscountType());
        p.setDiscountValue(req.getDiscountValue());
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setMinBookingAmount(req.getMinBookingAmount());
        p.setMaxDiscountAmount(req.getMaxDiscountAmount());
        p.setMinMembershipRank(req.getMinMembershipRank());
        p.setUsageLimit(req.getUsageLimit());

        if (req.getStatus() != null) p.setStatus(req.getStatus());
        else p.checkAndSetStatus();

        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    public void deletePromotion(int id) {
        Promotion p = promotionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        p.setStatus(PromotionStatus.DELETED);
        promotionRepository.save(p);
    }

    public PromotionResponseDTO toggleStatus(int id) {
        Promotion p = promotionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        if(p.getStatus() == PromotionStatus.DELETED) throw new BadRequestException("Cannot toggle deleted promo");

        if (p.getStatus() == PromotionStatus.PAUSED) {
            p.checkAndSetStatus(); // Logic tự check ngày để set Active/Expired
        } else {
            p.setStatus(PromotionStatus.PAUSED);
        }
        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getPromotionsByProperty(int propertyId) {
        return promotionRepository.findByProperty_PropertyId(propertyId).stream()
                .filter(p -> p.getStatus() != PromotionStatus.DELETED)
                .map(PromotionResponseDTO::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getAllGlobalPromotions() {
        return promotionRepository.findByPropertyIsNull().stream()
                .filter(p -> p.getStatus() != PromotionStatus.DELETED)
                .map(PromotionResponseDTO::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PromotionResponseDTO getById(int id) {
        return new PromotionResponseDTO(promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found")));
    }

    public PromotionResponseDTO uploadBanner(int promotionId, MultipartFile file) {
        Promotion p = promotionRepository.findById(promotionId).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        if (p.getBannerUrl() != null && !p.getBannerUrl().isEmpty()) fileStorageService.deleteFile(p.getBannerUrl());
        p.setBannerUrl(fileStorageService.storeImageFile(file, "campaign-images"));
        return new PromotionResponseDTO(promotionRepository.save(p));
    }
}