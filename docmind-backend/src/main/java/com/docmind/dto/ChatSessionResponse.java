package com.docmind.dto;

import com.docmind.entity.ConversationSession;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatSessionResponse {
    private Long id;
    private LocalDateTime createdAt;

    public static ChatSessionResponse from(ConversationSession session) {
        return new ChatSessionResponse(session.getId(), session.getCreatedAt());
    }
}
