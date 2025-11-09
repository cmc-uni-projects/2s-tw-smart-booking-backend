package com.example.smart_booking_system.dto.request.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class OwnerApplicationSubmitDTO {

    @NotBlank(message = "Địa chỉ thường trú không được để trống")
    private String permanentAddress;

    @NotBlank(message = "Quê quán không được để trống")
    private String hometownAddress;

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