package com.example.smart_booking_system.dto.request.admin;

import com.example.smart_booking_system.enums.MembershipRank;
import lombok.Data;

@Data
public class UpdateMembershipDTO {
    private Integer points; // Điểm số (cộng thêm hoặc set mới)
    private MembershipRank rank; // Hạng mới (nếu muốn set cứng)
    private boolean isAddPoints; // true: Cộng dồn, false: Ghi đè
}