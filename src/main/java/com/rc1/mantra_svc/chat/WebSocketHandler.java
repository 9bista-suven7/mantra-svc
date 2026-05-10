package com.rc1.mantra_svc.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc1.mantra_svc.dto.chat.*;
import com.rc1.mantra_svc.security.JwtUtil;
import com.rc1.mantra_svc.service.chat.ChatSessionRegistry;
import com.rc1.mantra_svc.service.chat.ConversationService;
import com.rc1.mantra_svc.service.chat.GroupService;
import com.rc1.mantra_svc.service.chat.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Reactive WebSocket handler for /ws/chat?token=<jwt>.
 *
 * Inbound JSON message structure:
 * {
 *   "action": "SEND_MESSAGE" | "TYPING_START" | "TYPING_STOP" | "MARK_READ",
 *   "payload": { ... }
 * }
 *
 * Outbound events are pushed via ChatSessionRegistry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler implements org.springframework.web.reactive.socket.WebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ChatSessionRegistry registry;
    private final PresenceService presenceService;
    private final ConversationService conversationService;
    private final GroupService groupService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // Extract JWT from query param: /ws/chat?token=xxx
        String token = extractToken(session);
        if (token == null) {
            return session.close();
        }

        // Extract email (JWT subject) for validation, userId (MongoDB ObjectId) for registry/presence.
        // Sessions must be keyed by MongoDB userId so that registry.sendToUser(participantId, …)
        // and presenceService.isOnline(userId) resolve correctly.
        String userId;
        try {
            String email = jwtUtil.extractEmail(token);
            if (!jwtUtil.isTokenValid(token, email)) return session.close();
            userId = jwtUtil.extractUserId(token);
            if (userId == null) {
                log.warn("WS auth failed: userId claim missing in token");
                return session.close();
            }
        } catch (Exception e) {
            log.warn("WS auth failed: {}", e.getMessage());
            return session.close();
        }

        final String finalUserId = userId;

        registry.register(finalUserId, session);

        Mono<Void> presenceOnline = presenceService.markOnline(finalUserId, session.getId())
            .onErrorResume(e -> {
                log.error("Presence mark-online failed: {}", e.getMessage());
                return Mono.empty();
            });

        // Handle inbound messages
        Mono<Void> inbound = session.receive()
            .flatMap(msg -> processInbound(finalUserId, msg.getPayloadAsText()))
            .onErrorResume(e -> {
                log.error("WS inbound error for {}: {}", finalUserId, e.getMessage());
                return Mono.empty();
            })
            .then();

        // Pipe outbound messages from registry sink to this session
        Mono<Void> outbound = session.send(
            registry.outboundFlux(session.getId())
                .map(session::textMessage)
        );

        return presenceOnline
            .then(Mono.when(inbound, outbound))
            .doFinally(sig -> {
                registry.unregister(session);
                presenceService.markOffline(finalUserId)
                    .subscribe(null, e -> log.error("Presence mark-offline failed: {}", e.getMessage()));
                log.debug("WS session closed for user {} signal={}", finalUserId, sig);
            });
    }

    private Mono<Void> processInbound(String userId, String rawJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> envelope = objectMapper.readValue(rawJson, Map.class);
            String action = (String) envelope.get("action");
            Object payload = envelope.get("payload");
            String payloadJson = objectMapper.writeValueAsString(payload);

            return switch (action) {
                case "SEND_MESSAGE" -> {
                    SendMessageRequest req = objectMapper.readValue(payloadJson, SendMessageRequest.class);
                    if (req.getConversationId() != null) {
                        yield conversationService.sendMessage(userId, req)
                            .flatMap(dto -> {
                                // Deliver to both participants
                                ChatEvent<MessageDto> ev = ChatEvent.of(
                                    ChatEvent.EventType.NEW_MESSAGE, req.getConversationId(), dto);
                                return conversationService
                                    .getOrCreateConversation(userId, getOtherParticipant(req, userId))
                                    .doOnSuccess(conv -> {
                                        registry.sendToUser(userId, ev);
                                        registry.sendToUser(conv.getOtherUser().getId(), ev);
                                    })
                                    .then();
                            });
                    } else {
                        yield groupService.sendGroupMessage(userId, req).then();
                    }
                }
                case "TYPING_START" -> {
                    TypingEvent ev = objectMapper.readValue(payloadJson, TypingEvent.class);
                    ev.setUserId(userId);
                    ev.setTyping(true);
                    broadcastTyping(ev);
                    yield Mono.empty();
                }
                case "TYPING_STOP" -> {
                    TypingEvent ev = objectMapper.readValue(payloadJson, TypingEvent.class);
                    ev.setUserId(userId);
                    ev.setTyping(false);
                    broadcastTyping(ev);
                    yield Mono.empty();
                }
                case "MARK_READ" -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> p = objectMapper.readValue(payloadJson, Map.class);
                    String targetId = p.get("targetId");
                    boolean isGroup = Boolean.parseBoolean(p.getOrDefault("group", "false"));
                    yield isGroup
                        ? groupService.markGroupRead(targetId, userId)
                        : conversationService.markAsRead(targetId, userId);
                }
                default -> {
                    log.warn("Unknown WS action: {}", action);
                    yield Mono.empty();
                }
            };
        } catch (Exception e) {
            log.error("Failed to process WS message: {}", e.getMessage());
            return Mono.empty();
        }
    }

    private void broadcastTyping(TypingEvent ev) {
        ChatEvent<TypingEvent> chatEvent = ChatEvent.of(
            ev.isTyping() ? ChatEvent.EventType.TYPING_START : ChatEvent.EventType.TYPING_STOP,
            ev.getTargetId(), ev);
        // For direct chats and groups — push to all except sender
        // In a real app you'd look up group members; simplified broadcast here
        registry.sendToUser(ev.getTargetId(), chatEvent); // targetId doubles as recipientId for 1:1
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) return kv[1];
        }
        return null;
    }

    /** Placeholder — real impl resolves from conversationId. */
    private String getOtherParticipant(SendMessageRequest req, String userId) {
        return ""; // resolved via conversation participants in service
    }
}
