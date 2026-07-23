package com.docmind.repository;

import com.docmind.entity.ConversationSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, Long> {
}
