package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT c FROM ChatMessage c WHERE c.userId = :userId ORDER BY c.createdAt ASC")
    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(@Param("userId") String userId);
}