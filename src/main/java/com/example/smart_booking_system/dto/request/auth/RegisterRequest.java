package com.example.smart_booking_system.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "{validation.fullName.required}")
    @Size(min = 2, max = 255, message = "{validation.fullName.size}")
    private String fullName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 255, message = "{validation.email.size}")
    private String email;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 100, message = "{validation.password.size}")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "{validation.password.pattern}"
    )
    private String password;

    @NotBlank(message = "{validation.password.confirmRequired}")
    private String confirmPassword;

    // === ĐÃ XÓA TRƯỜNG phoneNumber TẠI ĐÂY ===

    /**
     * Validate if password and confirmPassword match
     */
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}