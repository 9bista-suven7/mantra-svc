package com.rc1.mantra_svc.service.chat;

import com.rc1.mantra_svc.dto.chat.ChatEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session registry and message broadcast bus.
 * Maps userId -> set of active WebSocket sessions (supports multiple tabs/devices).
 * Uses Project Reactor Sinks for fan-out delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionRegistry {

    private final ObjectMapper objectMapper;

    /** userId -> sessions */
    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    /** sessionId -> userId (for reverse lookup on disconnect) */
    private final Map<String, String> sessionUser = new ConcurrentHashMap<>();

    /** Per-session sink for pushing messages to that specific WebSocket. */
    private final Map<String, Sinks.Many<String>> sessionSinks = new ConcurrentHashMap<>();

    public void register(String userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionUser.put(session.getId(), userId);
        sessionSinks.put(session.getId(),
            Sinks.many().unicast().onBackpressureBuffer());
        log.debug("Registered WS session {} for user {}", session.getId(), userId);
    }

    public void unregister(WebSocketSession session) {
        String userId = sessionUser.remove(session.getId());
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
        sessionSinks.remove(session.getId());
        log.debug("Unregistered WS session {}", session.getId());
    }

    public boolean isOnline(String userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Push a ChatEvent to all sessions of a specific user.
     */
    public <T> void sendToUser(String userId, ChatEvent<T> event) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;

        String json = toJson(event);
        if (json == null) return;

        sessions.forEach(session -> {
            Sinks.Many<String> sink = sessionSinks.get(session.getId());
            if (sink != null) {
                sink.tryEmitNext(json);
            }
        });
    }

    /**
     * Push a ChatEvent to all sessions of multiple users.
     */
    public <T> void broadcast(Iterable<String> userIds, ChatEvent<T> event) {
        userIds.forEach(uid -> sendToUser(uid, event));
    }

    /**
     * Returns a Flux of outbound JSON for a given session (consumed by the WebSocketHandler).
     */
    public Flux<String> outboundFlux(String sessionId) {
        Sinks.Many<String> sink = sessionSinks.get(sessionId);
        return sink != null ? sink.asFlux() : Flux.empty();
    }

    private <T> String toJson(T obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", e.getMessage());
            return null;
        }
    }
}
