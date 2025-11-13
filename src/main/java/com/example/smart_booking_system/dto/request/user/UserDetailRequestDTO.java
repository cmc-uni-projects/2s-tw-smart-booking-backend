package com.example.smart_booking_system.dto.request.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDetailRequestDTO {

    @Size(min = 2, max = 255, message = "Họ và tên phải từ 2 đến 255 ký tự")
    private String fullName; // Thêm trường này

    @Pattern(regexp = "(84|0[3|5|7|8|9])+([0-9]{8})\\b", message = "Số điện thoại không hợp lệ")
    private String phoneNumber; // Thêm trường này

    @Size(max = 50, message = "Giới tính phải ít hơn 50 ký tự")
    private String gender;

    @Size(max = 512, message = "URL ảnh phải ít hơn 512 ký tự")
    private String profilePhotoUrl;

    private String address;

    @Size(max = 100, message = "Thành phố phải ít hơn 100 ký tự")
    private String city;

    @Size(max = 100, message = "Quốc gia phải ít hơn 100 ký tự")
    private String country;
}