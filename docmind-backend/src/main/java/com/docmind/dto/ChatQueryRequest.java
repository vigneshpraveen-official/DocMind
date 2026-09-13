package com.docmind.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatQueryRequest {
    @NotNull(message = "sessionId must not be null")
    private Long sessionId;

    @NotBlank(message = "question must not be blank")
    private String question;
}
