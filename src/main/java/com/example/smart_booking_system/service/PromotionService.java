package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.promotion.PromotionRequestDTO;
import com.example.smart_booking_system.dto.PromotionResponseDTO;
import com.example.smart_booking_system.entity.Promotion;
import com.example.smart_booking_system.enums.DiscountType;
import com.example.smart_booking_system.enums.MembershipRank;
import com.example.smart_booking_system.enums.PromotionStatus;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.PromotionRepository;
import com.example.smart_booking_system.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final FileStorageService fileStorageService;
    private final BookingRepository bookingRepository;

    // 1. TẠO MỚI
    public PromotionResponseDTO createPromotion(PromotionRequestDTO req) {
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
        p.setUsageLimit(req.getUsageLimit());
        p.setUsageCount(0);

        // Set status ban đầu
        if (req.getStatus() != null) {
            p.setStatus(req.getStatus());
        } else {
            p.checkAndSetStatus(); // Tự động ACTIVE hoặc EXPIRED
        }

        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    // 2. CẬP NHẬT
    public PromotionResponseDTO updatePromotion(int id, PromotionRequestDTO req) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        // ✅ LOGIC MỚI: Nếu đã XÓA thì không cho sửa
        if (p.getStatus() == PromotionStatus.DELETED) {
            throw new BadRequestException("Không thể cập nhật mã khuyến mãi đã bị xóa.");
        }

        if (!p.getCode().equalsIgnoreCase(req.getCode())) {
            if (promotionRepository.existsByCode(req.getCode())) {
                throw new BadRequestException("Mã khuyến mãi '" + req.getCode() + "' đã tồn tại.");
            }
            p.setCode(req.getCode().toUpperCase());
        }

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

        // Nếu admin muốn đổi status trực tiếp
        if (req.getStatus() != null) {
            p.setStatus(req.getStatus());
        } else {
            // Nếu không, hệ thống tự check lại ngày (nếu đang Active/Expired)
            p.checkAndSetStatus();
        }

        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    // 3. XÓA MỀM (CHUYỂN SANG DELETED)
    public void deletePromotion(int id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        // Chuyển trạng thái sang DELETED
        p.setStatus(PromotionStatus.DELETED);
        promotionRepository.save(p);
    }

    // 4. BẬT/TẮT (TOGGLE: ACTIVE <-> PAUSED)
    public PromotionResponseDTO toggleStatus(int id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        // ✅ LOGIC MỚI: Nếu đã XÓA thì không cho Bật/Tắt
        if (p.getStatus() == PromotionStatus.DELETED) {
            throw new BadRequestException("Không thể thay đổi trạng thái của mã đã bị xóa.");
        }

        if (p.getStatus() == PromotionStatus.PAUSED) {
            // Đang Tắt -> Bật (Check ngày để xem Active hay Expired)
            LocalDateTime now = LocalDateTime.now();
            if (p.getEndDate().isBefore(now)) {
                p.setStatus(PromotionStatus.EXPIRED);
            } else {
                p.setStatus(PromotionStatus.ACTIVE);
            }
        } else {
            // Đang Active hoặc Expired -> Tắt (PAUSED)
            p.setStatus(PromotionStatus.PAUSED);
        }

        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    // 5. LẤY DANH SÁCH (Ẩn các mã đã xóa DELETED)
    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getAllGlobalPromotions() {
        // Lấy tất cả ngoại trừ DELETED (hoặc lấy tất cả tùy bạn, ở đây tôi lọc bỏ DELETED cho gọn)
        return promotionRepository.findAll().stream()
                .filter(p -> p.getStatus() != PromotionStatus.DELETED)
                .map(PromotionResponseDTO::new)
                .collect(Collectors.toList());
    }

    // 6. CHI TIẾT
    @Transactional(readOnly = true)
    public PromotionResponseDTO getById(int id) {
        Promotion p = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        return new PromotionResponseDTO(p);
    }

    // ============================================================
    // ✅ HÀM MỚI: UPLOAD BANNER (Logic 1 ảnh duy nhất)
    // ============================================================
    public PromotionResponseDTO uploadBanner(int promotionId, MultipartFile file) {
        // 1. Tìm khuyến mãi
        Promotion p = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        // 2. Nếu đang có banner cũ -> Xóa file vật lý đi để dọn rác
        if (p.getBannerUrl() != null && !p.getBannerUrl().isEmpty()) {
            fileStorageService.deleteFile(p.getBannerUrl());
        }

        // 3. Lưu file mới
        String newBannerPath = fileStorageService.storeImageFile(file, "campaign-images");

        // 4. Cập nhật đường dẫn vào DB
        p.setBannerUrl(newBannerPath);

        return new PromotionResponseDTO(promotionRepository.save(p));
    }

    /**
     * Gợi ý mã giảm giá tốt nhất cho người dùng với đơn hàng dự kiến
     *
     * @param userId        ID người dùng
     * @param bookingAmount Giá trị đơn đặt phòng dự kiến
     * @return PromotionResponseDTO tốt nhất hoặc null nếu không có
     */
    public PromotionResponseDTO suggestBestPromotion(String userId, BigDecimal bookingAmount) {
        // 1. Xác định hạng thành viên hiện tại của User
        MembershipRank userRank = calculateUserRank(userId);

        // 2. Lấy tất cả mã đang active
        List<Promotion> activePromotions = promotionRepository.findAvailablePromotions(LocalDateTime.now());

        // 3. Lọc và tìm mã giảm giá sâu nhất
        Promotion bestPromotion = activePromotions.stream()
                // Lọc 1: Kiểm tra hạng thành viên (User rank phải >= Promo rank)
                .filter(p -> isRankEligible(userRank, p.getMinMembershipRank()))
                // Lọc 2: Kiểm tra giá trị đơn hàng tối thiểu
                .filter(p -> p.getMinBookingAmount() == null || bookingAmount.compareTo(p.getMinBookingAmount()) >= 0)
                // Sắp xếp: Tính toán số tiền giảm thực tế và sort giảm dần (nhiều tiền nhất lên đầu)
                .sorted(Comparator.comparing((Promotion p) -> calculateDiscountAmount(p, bookingAmount)).reversed())
                .findFirst()
                .orElse(null);

        if (bestPromotion == null) {
            return null; // Không có mã nào phù hợp
        }

        return new PromotionResponseDTO(bestPromotion);
    }

    // --- CÁC HÀM HELPER ---

    // Tính Rank dựa trên tiền đã tiêu
    private MembershipRank calculateUserRank(String userId) {
        BigDecimal totalSpent = bookingRepository.calculateTotalSpentByUser(userId);
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;

        // Logic xếp hạng (Ví dụ theo đơn vị VNĐ)
        // Bạn có thể đưa các số này vào Constant hoặc Config
        if (totalSpent.compareTo(new BigDecimal("10000000")) >= 0) { // > 10 triệu
            return MembershipRank.DIAMOND;
        } else if (totalSpent.compareTo(new BigDecimal("5000000")) >= 0) { // > 5 triệu
            return MembershipRank.GOLD;
        } else if (totalSpent.compareTo(new BigDecimal("1000000")) >= 0) { // > 1 triệu
            return MembershipRank.SILVER;
        } else {
            return MembershipRank.BRONZE;
        }
    }

    // So sánh Rank (Enum Ordinal: BRONZE=0, SILVER=1, GOLD=2, DIAMOND=3)
    private boolean isRankEligible(MembershipRank userRank, MembershipRank requiredRank) {
        if (requiredRank == null) return true; // Không yêu cầu rank
        return userRank.ordinal() >= requiredRank.ordinal();
    }

    // Tính số tiền được giảm thực tế để so sánh
    private BigDecimal calculateDiscountAmount(Promotion p, BigDecimal bookingAmount) {
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (p.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discountAmount = p.getDiscountValue();
        } else if (p.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = bookingAmount.multiply(p.getDiscountValue()).divide(BigDecimal.valueOf(100));
            // Kiểm tra giới hạn giảm tối đa (nếu có)
            if (p.getMaxDiscountAmount() != null && discountAmount.compareTo(p.getMaxDiscountAmount()) > 0) {
                discountAmount = p.getMaxDiscountAmount();
            }
        }

        // Không được giảm quá giá trị đơn hàng
        return discountAmount.min(bookingAmount);
    }
}