package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.dto.chat.ConversationDto;
import com.rc1.mantra_svc.dto.chat.MessageDto;
import com.rc1.mantra_svc.dto.chat.SendMessageRequest;
import com.rc1.mantra_svc.security.UserPrincipal;
import com.rc1.mantra_svc.service.chat.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST API for direct (1-to-1) conversations and their messages.
 *
 * GET  /api/chat/conversations              — list conversations
 * POST /api/chat/conversations/{userId}     — get or create conversation with user
 * GET  /api/chat/conversations/{id}/messages — message history (paginated)
 * POST /api/chat/conversations/{id}/messages — send message
 * PUT  /api/chat/conversations/{id}/read    — mark all as read
 * DELETE /api/chat/messages/{id}            — delete a message
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/conversations")
    public Mono<List<ConversationDto>> listConversations(
            @AuthenticationPrincipal UserPrincipal principal) {
        return conversationService.listConversations(principal.getUserId()).collectList();
    }

    @PostMapping("/conversations/with/{otherUserId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ConversationDto> getOrCreate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String otherUserId) {
        return conversationService.getOrCreateConversation(principal.getUserId(), otherUserId);
    }

    @GetMapping("/conversations/{id}/messages")
    public Mono<List<MessageDto>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @RequestParam(required = false) String before,
            @RequestParam(defaultValue = "0") int page) {
        return conversationService.getMessageHistory(id, before, page).collectList();
    }

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MessageDto> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest req) {
        req.setConversationId(id);
        return conversationService.sendMessage(principal.getUserId(), req);
    }

    @PutMapping("/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return conversationService.markAsRead(id, principal.getUserId());
    }

    @DeleteMapping("/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String messageId) {
        return conversationService.deleteMessage(messageId, principal.getUserId()).then();
    }

    /** Hide a 1-to-1 conversation from the requester's sidebar. */
    @DeleteMapping("/conversations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> hideConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return conversationService.hideConversation(id, principal.getUserId());
    }
}
