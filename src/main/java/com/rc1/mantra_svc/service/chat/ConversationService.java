package com.rc1.mantra_svc.service.chat;

import com.rc1.mantra_svc.dto.chat.*;
import com.rc1.mantra_svc.exception.ResourceNotFoundException;
import com.rc1.mantra_svc.model.chat.Conversation;
import com.rc1.mantra_svc.model.chat.Message;
import com.rc1.mantra_svc.model.chat.MessageStatus;
import com.rc1.mantra_svc.model.chat.MessageType;
import com.rc1.mantra_svc.repository.UserRepository;
import com.rc1.mantra_svc.repository.chat.ConversationRepository;
import com.rc1.mantra_svc.repository.chat.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles direct (1-to-1) conversation lifecycle and message delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatSessionRegistry registry;
    private final PresenceService presenceService;

    private static final int PAGE_SIZE = 30;

    /**
     * Get or create a conversation between two users.
     */
    public Mono<ConversationDto> getOrCreateConversation(String requesterId, String otherUserId) {
        List<String> ids = Arrays.asList(requesterId, otherUserId);
        Collections.sort(ids);

        return conversationRepository.findByParticipants(ids)
            .switchIfEmpty(Mono.defer(() -> {
                Conversation conv = Conversation.builder()
                    .participantIds(ids)
                    .unreadCounts(new HashMap<>(Map.of(requesterId, 0, otherUserId, 0)))
                    .build();
                return conversationRepository.save(conv);
            }))
            .flatMap(conv -> enrichConversationDto(conv, requesterId));
    }

    /**
     * List all conversations for a user.
     */
    public Flux<ConversationDto> listConversations(String userId) {
        return conversationRepository.findByParticipantId(userId)
            .filter(conv -> conv.getHiddenFor() == null || !conv.getHiddenFor().contains(userId))
            .flatMap(conv -> enrichConversationDto(conv, userId));
    }

    /**
     * Hide a conversation from the requester's list. Will automatically reappear
     * when a new message arrives for them.
     */
    public Mono<Void> hideConversation(String conversationId, String userId) {
        return conversationRepository.findById(conversationId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Conversation not found")))
            .flatMap(conv -> {
                if (!conv.getParticipantIds().contains(userId)) {
                    return Mono.error(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN));
                }
                if (conv.getHiddenFor() == null) conv.setHiddenFor(new HashSet<>());
                conv.getHiddenFor().add(userId);
                if (conv.getUnreadCounts() != null) conv.getUnreadCounts().put(userId, 0);
                return conversationRepository.save(conv).then();
            });
    }

    /**
     * Send a message in a direct conversation.
     */
    public Mono<MessageDto> sendMessage(String senderId, SendMessageRequest req) {
        return conversationRepository.findById(req.getConversationId())
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Conversation not found")))
            .flatMap(conv -> {
                Message msg = Message.builder()
                    .conversationId(conv.getId())
                    .senderId(senderId)
                    .type(req.getType())
                    .content(req.getContent())
                    .status(MessageStatus.SENT)
                    .replyToMessageId(req.getReplyToMessageId())
                    .readReceipts(new ArrayList<>())
                    .build();

                return messageRepository.save(msg)
                    .flatMap(saved -> {
                        // Update last message snapshot and unread counts
                        Conversation.MessageSummary summary = Conversation.MessageSummary.builder()
                            .messageId(saved.getId())
                            .senderId(senderId)
                            .content(req.getType() == MessageType.TEXT ? req.getContent() : "[" + req.getType() + "]")
                            .type(req.getType())
                            .sentAt(saved.getCreatedAt())
                            .build();

                        conv.setLastMessage(summary);
                        conv.getParticipantIds().forEach(pid -> {
                            if (!pid.equals(senderId)) {
                                conv.getUnreadCounts().merge(pid, 1, Integer::sum);
                            }
                        });
                        // Re-show conversation for anyone who had hidden it
                        if (conv.getHiddenFor() != null) conv.getHiddenFor().clear();

                        return conversationRepository.save(conv).then(enrichMessageDto(saved));
                    })
                    .doOnSuccess(dto -> {
                        // Broadcast NEW_MESSAGE to all conversation participants in real-time
                        ChatEvent<MessageDto> ev = ChatEvent.of(
                            ChatEvent.EventType.NEW_MESSAGE, conv.getId(), dto);
                        conv.getParticipantIds().forEach(pid -> registry.sendToUser(pid, ev));
                    });
            });
    }

    /**
     * Load paginated message history.
     */
    public Flux<MessageDto> getMessageHistory(String conversationId, String before, int page) {
        PageRequest pageReq = PageRequest.of(page, PAGE_SIZE);
        Flux<Message> messages = before != null
            ? messageRepository.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                conversationId, Instant.parse(before), pageReq)
            : messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageReq);

        return messages.flatMap(this::enrichMessageDto);
    }

    /**
     * Mark all unread messages in a conversation as read by the given user.
     */
    public Mono<Void> markAsRead(String conversationId, String userId) {
        return conversationRepository.findById(conversationId)
            .flatMap(conv -> {
                conv.getUnreadCounts().put(userId, 0);
                return conversationRepository.save(conv);
            })
            .then(
                messageRepository.findByConversationIdOrderByCreatedAtDesc(
                    conversationId, PageRequest.of(0, 200))
                    .filter(msg -> !msg.getSenderId().equals(userId))
                    .filter(msg -> msg.getReadReceipts().stream()
                        .noneMatch(r -> r.getUserId().equals(userId)))
                    .flatMap(msg -> {
                        msg.getReadReceipts().add(
                            Message.ReadReceipt.builder()
                                .userId(userId)
                                .readAt(Instant.now())
                                .build());
                        msg.setStatus(MessageStatus.READ);
                        return messageRepository.save(msg)
                            .doOnSuccess(saved -> {
                                // Notify sender of READ status
                                ChatEvent<Map<String, String>> ev = ChatEvent.of(
                                    ChatEvent.EventType.MESSAGE_STATUS_UPDATE,
                                    conversationId,
                                    Map.of("messageId", saved.getId(), "status", "READ", "userId", userId));
                                registry.sendToUser(saved.getSenderId(), ev);
                            });
                    })
                    .then()
            );
    }

    /**
     * Delete a message (soft delete — content cleared, deleted flag set).
     */
    public Mono<MessageDto> deleteMessage(String messageId, String requesterId) {
        return messageRepository.findById(messageId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Message not found")))
            .filter(msg -> msg.getSenderId().equals(requesterId))
            .switchIfEmpty(Mono.error(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN)))
            .flatMap(msg -> {
                msg.setDeleted(true);
                msg.setContent(null);
                return messageRepository.save(msg);
            })
            .flatMap(msg -> enrichMessageDto(msg)
                .doOnSuccess(dto -> {
                    String targetId = msg.getConversationId() != null
                        ? msg.getConversationId() : msg.getGroupId();
                    ChatEvent<MessageDto> ev = ChatEvent.of(
                        ChatEvent.EventType.MESSAGE_DELETED, targetId, dto);
                    conversationRepository.findById(targetId)
                        .subscribe(conv -> conv.getParticipantIds().forEach(
                            pid -> registry.sendToUser(pid, ev)));
                }));
    }

    // ── Enrichment ──────────────────────────────────────────────────────────

    private Mono<ConversationDto> enrichConversationDto(Conversation conv, String requesterId) {
        String otherUserId = conv.getParticipantIds().stream()
            .filter(id -> !id.equals(requesterId))
            .findFirst().orElse(null);

        if (otherUserId == null) return Mono.empty();

        return userRepository.findById(otherUserId)
            .flatMap(other -> presenceService.isOnline(otherUserId)
                .map(online -> ConversationDto.builder()
                    .id(conv.getId())
                    .otherUser(ConversationDto.ParticipantDto.builder()
                        .id(other.getId())
                        .displayName(other.getDisplayName())
                        .username(other.getUsername())
                        .avatarColor(other.getAvatarColor())
                        .build())
                    .lastMessage(conv.getLastMessage())
                    .unreadCount(conv.getUnreadCounts()
                        .getOrDefault(requesterId, 0))
                    .otherUserOnline(online)
                    .updatedAt(conv.getUpdatedAt())
                    .build()));
    }

    public Mono<MessageDto> enrichMessageDto(Message msg) {
        return userRepository.findById(msg.getSenderId())
            .map(sender -> MessageDto.builder()
                .id(msg.getId())
                .conversationId(msg.getConversationId())
                .groupId(msg.getGroupId())
                .senderId(msg.getSenderId())
                .senderDisplayName(sender.getDisplayName())
                .senderAvatarColor(sender.getAvatarColor())
                .type(msg.getType())
                .content(msg.isDeleted() ? null : msg.getContent())
                .status(msg.getStatus())
                .replyToMessageId(msg.getReplyToMessageId())
                .deleted(msg.isDeleted())
                .editedContent(msg.getEditedContent())
                .editedAt(msg.getEditedAt())
                .createdAt(msg.getCreatedAt())
                .readReceipts(msg.getReadReceipts() != null
                    ? msg.getReadReceipts().stream()
                        .map(r -> MessageDto.ReadReceiptDto.builder()
                            .userId(r.getUserId())
                            .readAt(r.getReadAt())
                            .build())
                        .collect(Collectors.toList())
                    : List.of())
                .build());
    }
}
