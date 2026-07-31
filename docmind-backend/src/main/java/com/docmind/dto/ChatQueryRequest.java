package com.docmind.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatQueryRequest {
    @NotBlank(message = "question must not be blank")
    private String question;
}
