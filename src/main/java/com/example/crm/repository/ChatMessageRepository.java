package com.example.crm.repository;

import com.example.crm.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Transactional
    void deleteByTimestampBefore(LocalDateTime timestamp);
}
