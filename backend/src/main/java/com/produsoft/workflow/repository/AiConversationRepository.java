package com.produsoft.workflow.repository;

import com.produsoft.workflow.domain.AiConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    List<AiConversation> findByCreatedByOrderByUpdatedAtDesc(String createdBy);

    @EntityGraph(attributePaths = "messages")
    Optional<AiConversation> findWithMessagesByIdAndCreatedBy(Long id, String createdBy);

    Optional<AiConversation> findByIdAndCreatedBy(Long id, String createdBy);
}
