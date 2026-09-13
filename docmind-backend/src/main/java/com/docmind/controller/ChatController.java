package com.docmind.controller;

import com.docmind.dto.ChatQueryRequest;
import com.docmind.dto.ChatQueryResponse;
import com.docmind.dto.ChatSessionResponse;
import com.docmind.dto.MessageResponse;
import com.docmind.service.auth.UserPrincipal;
import com.docmind.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionResponse> createSession(@AuthenticationPrincipal UserPrincipal principal) {
        var session = chatService.createSession(principal.user());
        return ResponseEntity.status(HttpStatus.CREATED).body(ChatSessionResponse.from(session));
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<MessageResponse> messages = chatService.getMessages(id, principal.user()).stream()
                .map(MessageResponse::from)
                .toList();
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/query")
    public ResponseEntity<ChatQueryResponse> query(
            @Valid @RequestBody ChatQueryRequest request, @AuthenticationPrincipal UserPrincipal principal
    ) {
        var response = chatService.query(request.getSessionId(), request.getQuestion(), principal.user());
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleForbidden(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }
}
