package com.example.smart_booking_system.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "{validation.token.required}")
    private String token;

    @NotBlank(message = "{validation.password.newRequired}")
    private String newPassword;

    @NotBlank(message = "{validation.password.confirmRequired}")
    private String confirmPassword;
}
