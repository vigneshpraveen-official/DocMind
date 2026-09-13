package com.docmind.dto;

import com.docmind.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private String role;
    private String content;
    private LocalDateTime createdAt;

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(), message.getRole().name(), message.getContent(), message.getCreatedAt());
    }
}
