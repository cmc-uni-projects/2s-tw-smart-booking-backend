package com.example.smart_booking_system.dto.request.property;

import com.example.smart_booking_system.enums.PropertyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class PropertyApplicationSubmitDTO {

    @NotNull(message = "{validation.property.typeRequired}")
    private PropertyType propertyType;

    @NotBlank(message = "{validation.property.countryRequired}")
    private String country;

    @NotBlank(message = "{validation.property.provinceRequired}")
    private String province;

    @NotBlank(message = "{validation.property.districtRequired}")
    private String city; // Quận/Huyện

    // ✅ [NEW] Thêm các trường code và ward
    private String ward;         // Phường/Xã (Có thể null nếu FE không bắt buộc)
    private String provinceCode;
    private String districtCode;
    private BigDecimal price;         // Giá ngày thường
    private BigDecimal weekendPrice;  // Giá cuối tuần
    private Integer capacity;         // Sức chứa
    private String unitName;          // Tên căn (VD: Villa A)
    @NotBlank(message = "{validation.property.addressRequired}")
    private String address;

    @NotBlank(message = "{validation.property.nameRequired}")
    @Size(min = 5, message = "{validation.property.nameMin}")
    private String propertyName;

    @NotBlank(message = "{validation.property.descriptionRequired}")
    @Size(min = 50, message = "{validation.property.descriptionMin}")
    private String description;

    @NotNull(message = "{validation.property.areaRequired}")
    @Min(value = 1, message = "{validation.property.areaPositive}")
    private BigDecimal area;

    private Map<String, Boolean> amenities;

    @NotNull(message = "{validation.terms.required}")
    private Boolean terms;

    private BigDecimal latitude;
    private BigDecimal longitude;
}