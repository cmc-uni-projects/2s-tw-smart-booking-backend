package com.example.smart_booking_system.dto.request.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@Data
public class OwnerApplicationSubmitDTO {

    // --- CÁC TRƯỜNG BỔ SUNG TỪ FRONTEND ---
    @NotBlank(message = "Họ tên không được để trống")
    private String personalFullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String personalEmail;

    @NotBlank(message = "SĐT không được để trống")
    private String personalPhone;

    @NotBlank(message = "Số CCCD/Passport không được để trống")
    private String personalIdCard;

    private LocalDate personalDob; // Backend sẽ tự parse từ ISO String "yyyy-MM-dd"

    // --- CÁC TRƯỜNG ĐÃ CÓ (Đổi tên cho khớp) ---
    @NotBlank(message = "Địa chỉ thường trú không được để trống")
    private String permanentAddress; // Frontend đang gửi 'personalAddress'

    @NotBlank(message = "Quê quán không được để trống")
    private String hometownAddress; // Frontend đang gửi 'personalHometown'

    @NotBlank(message = "Ảnh mặt trước CCCD không được để trống")
    @URL(message = "URL ảnh mặt trước không hợp lệ")
    private String cardFrontImage;

    @NotBlank(message = "Ảnh mặt sau CCCD không được để trống")
    @URL(message = "URL ảnh mặt sau không hợp lệ")
    private String cardBackImage;

    @NotBlank(message = "Ảnh giấy phép kinh doanh không được để trống")
    @URL(message = "URL ảnh giấy phép không hợp lệ")
    private String businessLicenseImage;

    @NotBlank(message = "Số giấy phép kinh doanh không được để trống")
    @Size(min = 5, max = 100)
    private String businessLicenseNumber;
}