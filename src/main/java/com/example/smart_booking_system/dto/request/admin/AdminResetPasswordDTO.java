package com.example.smart_booking_system.dto.request.admin;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminResetPasswordDTO {

    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[@#$%^&+=!._-])(?=\\S+$).{8,}$",
            message = "Mật khẩu phải có ít nhất 8 ký tự, bao gồm 1 chữ cái viết hoa và 1 ký tự đặc biệt"
    )
    private String newPassword;
}