package com.example.smart_booking_system.repository;

import com.example.smart_booking_system.entity.AiChatHistory;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

//file này để lấy lịch sử chat
@Repository
public interface AiChatHistoryRepository extends JpaRepository<AiChatHistory, Integer> {

    @Query(value = """
        SELECT * FROM ai_chat_history 
        WHERE userId = :userId 
        ORDER BY timestamp DESC 
        LIMIT 20
    """, nativeQuery = true)
    List<AiChatHistory> findRecentHistoryByUserId(@Param("userId") String userId);

}
