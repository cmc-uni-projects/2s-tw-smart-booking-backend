package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.RatingRequestDTO;
import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.Rating;
import com.example.smart_booking_system.entity.RatingImage;
import com.example.smart_booking_system.enums.BookingStatus;
import com.example.smart_booking_system.enums.RatingType;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.RatingImageRepository;
import com.example.smart_booking_system.repository.RatingRepository;
import com.example.smart_booking_system.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final BookingRepository bookingRepository;
    private final RatingImageRepository ratingImageRepository;
    private final FileStorageService fileStorageService;

    private static final int PAGE_SIZE = 10;

    // --- Helper Methods ---

    private Rating attachImages(Rating rating) {
        if (rating == null) return null;
        List<RatingImage> imgs = ratingImageRepository.getImagesByRatingId(rating.getRatingId());
        rating.setImages(imgs);
        return rating;
    }

    private List<Rating> attachImages(List<Rating> list) {
        list.forEach(this::attachImages);
        return list;
    }

    private RatingType classify(int stars, String comment) {
        if (comment == null) comment = "";
        String lower = comment.toLowerCase();
        if (lower.contains("dm") || lower.contains("địt") || lower.contains("cút")
                || lower.contains("fuck") || lower.contains("shit")
                || lower.contains("bố mày") || lower.contains("óc chó")
                || lower.contains("ngu")|| lower.contains("lol")) {
            return RatingType.VIOLATION;
        }
        if (stars >= 4 || lower.contains("good") || lower.contains("tốt") ||
                lower.contains("great") || lower.contains("tuyệt") || lower.contains("hài lòng")) {
            return RatingType.POSITIVE;
        }
        if (stars <= 2 || lower.contains("bad") || lower.contains("tệ") ||
                lower.contains("kém") || lower.contains("không hài lòng")) {
            return RatingType.NEGATIVE;
        }
        return RatingType.NEGATIVE;
    }

    // --- Main Logic ---

    @Transactional
    public Rating createRating(RatingRequestDTO dto, List<MultipartFile> files) {
        // 1. Chặn spam
        if (ratingRepository.existsByBookingId_BookingId(dto.getBookingId())) {
            throw new RuntimeException("Bạn đã đánh giá đơn đặt phòng này rồi.");
        }

        // 2. Validate Booking
        Booking booking = bookingRepository.findById(dto.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new RuntimeException("Bạn chỉ có thể đánh giá khi chuyến đi đã hoàn thành.");
        }

        // 3. Phân loại & Lưu Rating
        RatingType type = classify(dto.getStars(), dto.getComment());
        if (type == RatingType.VIOLATION) throw new RuntimeException("Đánh giá chứa từ ngữ không phù hợp.");

        Rating rating = new Rating();
        rating.setBookingId(booking);
        rating.setUserId(booking.getUser());
        rating.setRating(dto.getStars());
        rating.setComment(dto.getComment());
        rating.setRatingType(type);
        rating.setHidden(false);

        rating.setImages(new ArrayList<>());

        Rating savedRating = ratingRepository.save(rating);

        // 4. Upload ảnh
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                // SỬA: Dùng đúng tên hàm storeImageFile
                String fileUrl = fileStorageService.storeImageFile(file, "ratingImage");

                RatingImage img = new RatingImage();
                img.setRating(savedRating);
                img.setImageUrl(fileUrl);

                savedRating.getImages().add(img);
            }
        }

        return savedRating;
    }

    @Transactional
    public Rating updateRating(int id, RatingRequestDTO dto, List<MultipartFile> files) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        rating.setRating(dto.getStars());
        rating.setComment(dto.getComment());

        RatingType type = classify(dto.getStars(), dto.getComment());
        if (type == RatingType.VIOLATION) throw new RuntimeException("Nội dung không phù hợp.");
        rating.setRatingType(type);

        // Xử lý ảnh
        if (files != null && !files.isEmpty()) {
            if (rating.getImages() == null) {
                rating.setImages(new ArrayList<>());
            }

            // Xóa ảnh cũ
            rating.getImages().clear();

            // Thêm ảnh mới
            for (MultipartFile file : files) {
                // SỬA: Dùng đúng tên hàm storeImageFile
                String fileUrl = fileStorageService.storeImageFile(file, "ratingImage");

                RatingImage img = new RatingImage();
                img.setRating(rating);
                img.setImageUrl(fileUrl);

                rating.getImages().add(img);
            }
        }

        return ratingRepository.save(rating);
    }

    public void deleteRating(int id) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        // Lấy thông tin người dùng đang đăng nhập
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String ownerEmail = rating.getUserId().getEmail(); // Hoặc getUsername() tuỳ config UserDetails của bạn

        if (!isAdmin && !currentUsername.equals(ownerEmail)) {
            throw new RuntimeException("Bạn không có quyền xóa đánh giá này.");
        }

        // Xóa ảnh (nếu cần thiết, dù orphanRemoval=true đã lo rồi)
        List<RatingImage> imgs = ratingImageRepository.getImagesByRatingId(id);
        ratingImageRepository.deleteAll(imgs);

        ratingRepository.deleteById(id);
    }

    // --- Getters & Pagination ---

    private Page<Rating> paginate(List<Rating> list, int page) {
        int start = page * PAGE_SIZE;

        if (start >= list.size()) {
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, PAGE_SIZE), list.size());
        }

        int end = Math.min(start + PAGE_SIZE, list.size());
        List<Rating> content = list.subList(start, end);
        attachImages(content);
        return new PageImpl<>(content, PageRequest.of(page, PAGE_SIZE), list.size());
    }

    public Page<Rating> getRatingsForProperty(int propertyId, int page) {
        List<Rating> all = ratingRepository.getRatingsByProperty(propertyId);

        List<Rating> good = all.stream().filter(r -> r.getRatingType() == RatingType.POSITIVE).toList();
        List<Rating> bad = all.stream().filter(r -> r.getRatingType() == RatingType.NEGATIVE).toList();

        int goodCount = Math.min((int) Math.ceil(all.size() * 0.8), good.size());
        int badCount = Math.min((int) Math.ceil(all.size() * 0.2), bad.size());

        List<Rating> mixed = new ArrayList<>();
        if (!good.isEmpty()) mixed.addAll(good.subList(0, goodCount));
        if (!bad.isEmpty()) mixed.addAll(bad.subList(0, badCount));

        Collections.shuffle(mixed);
        return paginate(mixed, page);
    }

    public Rating pinRating(int ratingId, boolean pin) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        // --- LOGIC CHECK QUYỀN OWNER ---
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String currentUserId = auth.getName(); // Hoặc lấy ID từ principal tuỳ config

        // Nếu không phải Admin, bắt buộc phải là Owner của khách sạn này
        if (!isAdmin) {
            // Lấy ID chủ sở hữu của khách sạn liên quan đến rating này
            // Giả sử Property có quan hệ với User (owner)
            String propertyOwnerId = String.valueOf(rating.getBookingId().getProperty().getOwner().getUserId());

            // Nếu ID người đang login KHÁC ID chủ khách sạn -> Chặn
            // (Lưu ý: Cần đảm bảo cách lấy currentUserId khớp với propertyOwnerId - cùng là username hoặc cùng là ID)

            // Ví dụ nếu Security lưu username là email:
            // String ownerEmail = rating.getBookingId().getProperty().getUser().getEmail();
            // if (!currentUserId.equals(ownerEmail)) throw ...

            // Ví dụ nếu Security lưu ID:
            if (!currentUserId.equals(propertyOwnerId)) {
                throw new RuntimeException("Bạn không có quyền ghim đánh giá của khách sạn này.");
            }
        }
        // -------------------------------

        if (pin) {
            int propertyId = rating.getBookingId().getProperty().getPropertyId();
            int currentPinnedCount = ratingRepository.countByBookingId_Property_PropertyIdAndIsPinnedTrue(propertyId);

            if (currentPinnedCount >= 3) {
                throw new RuntimeException("Chỉ được ghim tối đa 3 bình luận.");
            }
        }

        rating.setIsPinned(pin);
        return ratingRepository.save(rating); // Bỏ attachImages nếu không cần thiết để tối ưu
    }

    public Rating getRatingById(int id) {
        Rating rating = ratingRepository.findById(id).orElseThrow(() -> new RuntimeException("Rating not found"));
        if (rating.isHidden()) throw new RuntimeException("This rating is hidden.");
        return attachImages(rating);
    }

    public Page<Rating> getRatingByBooking(int bookingId, int page) {
        return paginate(ratingRepository.getRatingByBookingId(bookingId), page);
    }

    public Page<Rating> getRatingByUser(String userId, int page) {
        return paginate(ratingRepository.getRatingByUserId(userId), page);
    }

    public Page<Rating> getRatingByType(String ratingType, int page) {
        return paginate(ratingRepository.getRatingByRatingType(ratingType), page);
    }

    public Page<Rating> getHiddenRatings(int page) {
        return paginate(ratingRepository.getRatingByHidden(), page);
    }

    public Rating hideRating(int ratingId, boolean hide) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));
        rating.setHidden(hide);
        return ratingRepository.save(rating);
    }
}