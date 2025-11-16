package com.example.smart_booking_system.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class PropertyPoliciesRequestDTO {

    private int propertyId;

    private boolean petsAllowed;
    private String petPolicyDescription;

    private boolean smokingAllowed;
    private String smokingPolicyDescription;

    private boolean childrenAllowed;
    private String childrenPolicyDescription;

    private LocalTime checkInFrom;
    private LocalTime checkInTo;
    private LocalTime checkOutFrom;
    private LocalTime checkOutTo;

    private String quietHours;

    private boolean allowFreeCancellation;
    private Integer freeCancellationDays;
    private String cancellationPolicyDescription;

    private boolean requiresPrepayment;
    private String prepaymentPolicy;

    private boolean securityDepositRequired;
    private BigDecimal securityDepositAmount;
    private String securityDepositDescription;

    private Integer minimumAge;
}
