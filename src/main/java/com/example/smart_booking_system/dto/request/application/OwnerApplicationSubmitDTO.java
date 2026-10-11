package com.example.smart_booking_system.dto.request.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class OwnerApplicationSubmitDTO {

    @NotBlank(message = "{validation.fullName.required}")
    private String personalFullName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String personalEmail;

    @NotBlank(message = "{validation.phone.required}")
    private String personalPhone;

    @NotBlank(message = "{validation.idCard.required}")
    private String personalIdCard;

    private LocalDate personalDob;

    @NotBlank(message = "{validation.permanentAddress.required}")
    private String permanentAddress;

    @NotBlank(message = "{validation.hometown.required}")
    private String hometownAddress;

    // FE sẽ gửi file MultipartFile (multipart/form-data). Bắt buộc phải có file.
    @NotNull(message = "{validation.cardFront.required}")
    private MultipartFile cardFrontImage;

    @NotNull(message = "{validation.cardBack.required}")
    private MultipartFile cardBackImage;

    @NotNull(message = "{validation.licenseImage.required}")
    private MultipartFile businessLicenseImage;

    @NotBlank(message = "{validation.licenseNumber.required}")
    @Size(min = 5, max = 100)
    private String businessLicenseNumber;
}
