package com.example.smart_booking_system.service;


import com.example.smart_booking_system.dto.PropertyPoliciesResponseDTO;
import com.example.smart_booking_system.dto.request.PropertyPoliciesRequestDTO;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.PropertyPolicies;
import com.example.smart_booking_system.repository.PropertyPoliciesRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PropertyPoliciesService {

    private final PropertyPoliciesRepository policiesRepo;
    private final PropertyRepository propertyRepo;

    public PropertyPoliciesResponseDTO createOrUpdatePolicies(PropertyPoliciesRequestDTO req) {

        Property property = propertyRepo.findById(req.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Lấy policies theo propertyId
        PropertyPolicies policies = policiesRepo.findByPropertyId(req.getPropertyId());
        if (policies == null) {
            policies = new PropertyPolicies();
            policies.setPropertyId(property);
        }

        // Map fields
        policies.setPetsAllowed(req.isPetsAllowed());
        policies.setPetPolicyDescription(req.getPetPolicyDescription());

        policies.setSmokingAllowed(req.isSmokingAllowed());
        policies.setSmokingPolicyDescription(req.getSmokingPolicyDescription());

        policies.setChildrenAllowed(req.isChildrenAllowed());
        policies.setChildrenPolicyDescription(req.getChildrenPolicyDescription());

        policies.setCheckInFrom(req.getCheckInFrom());
        policies.setCheckInTo(req.getCheckInTo());
        policies.setCheckOutFrom(req.getCheckOutFrom());
        policies.setCheckOutTo(req.getCheckOutTo());

        policies.setQuietHours(req.getQuietHours());

        policies.setAllowFreeCancellation(req.isAllowFreeCancellation());
        policies.setFreeCancellationDays(req.getFreeCancellationDays());
        policies.setCancellationPolicyDescription(req.getCancellationPolicyDescription());

        policies.setRequiresPrepayment(req.isRequiresPrepayment());
        policies.setPrepaymentPolicy(req.getPrepaymentPolicy());

        policies.setSecurityDepositRequired(req.isSecurityDepositRequired());
        policies.setSecurityDepositAmount(req.getSecurityDepositAmount());
        policies.setSecurityDepositDescription(req.getSecurityDepositDescription());

        policies.setMinimumAge(req.getMinimumAge());

        policiesRepo.save(policies);

        return convertToDTO(policies);
    }

    public PropertyPoliciesResponseDTO getPoliciesByPropertyId(int propertyId) {
        PropertyPolicies policies = policiesRepo.findByPropertyId(propertyId);
        if (policies == null) {
            throw new RuntimeException("Policies not found for propertyId " + propertyId);
        }
        return convertToDTO(policies);
    }

    private PropertyPoliciesResponseDTO convertToDTO(PropertyPolicies p) {

        PropertyPoliciesResponseDTO dto = new PropertyPoliciesResponseDTO();

        dto.setPolicyId(p.getPolicyId());
        dto.setPropertyId(p.getPropertyId().getPropertyId());

        dto.setPetsAllowed(p.isPetsAllowed());
        dto.setPetPolicyDescription(p.getPetPolicyDescription());

        dto.setSmokingAllowed(p.isSmokingAllowed());
        dto.setSmokingPolicyDescription(p.getSmokingPolicyDescription());

        dto.setChildrenAllowed(p.isChildrenAllowed());
        dto.setChildrenPolicyDescription(p.getChildrenPolicyDescription());

        dto.setCheckInFrom(p.getCheckInFrom());
        dto.setCheckInTo(p.getCheckInTo());
        dto.setCheckOutFrom(p.getCheckOutFrom());
        dto.setCheckOutTo(p.getCheckOutTo());

        dto.setQuietHours(p.getQuietHours());

        dto.setAllowFreeCancellation(p.isAllowFreeCancellation());
        dto.setFreeCancellationDays(p.getFreeCancellationDays());
        dto.setCancellationPolicyDescription(p.getCancellationPolicyDescription());

        dto.setRequiresPrepayment(p.isRequiresPrepayment());
        dto.setPrepaymentPolicy(p.getPrepaymentPolicy());

        dto.setSecurityDepositRequired(p.isSecurityDepositRequired());
        dto.setSecurityDepositAmount(p.getSecurityDepositAmount());
        dto.setSecurityDepositDescription(p.getSecurityDepositDescription());

        dto.setMinimumAge(p.getMinimumAge());

        return dto;
    }
}
