package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Integer> {
    Optional<RefundRequest> findByPayment_PaymentId(Integer paymentId);
}