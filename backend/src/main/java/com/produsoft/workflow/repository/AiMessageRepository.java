package com.produsoft.workflow.repository;

import com.produsoft.workflow.domain.AiMessage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {
    Optional<AiMessage> findTopByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
