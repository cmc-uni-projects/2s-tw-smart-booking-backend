package com.example.smart_booking_system.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "{validation.password.oldRequired}")
    private String oldPassword;

    @NotBlank(message = "{validation.password.newRequired}")
    private String newPassword;

    @NotBlank(message = "{validation.password.confirmRequired}")
    private String confirmPassword;
}
