package com.rc1.mantra_svc.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket event envelope — wraps all real-time push payloads.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEvent<T> {

    public enum EventType {
        NEW_MESSAGE,
        MESSAGE_STATUS_UPDATE,
        TYPING_START,
        TYPING_STOP,
        PRESENCE_UPDATE,
        GROUP_MEMBER_ADDED,
        GROUP_MEMBER_REMOVED,
        GROUP_UPDATED,
        MESSAGE_DELETED,
        MESSAGE_EDITED
    }

    private EventType type;
    private String targetId;   // conversationId or groupId
    private T payload;

    /** Factory helpers */
    public static <T> ChatEvent<T> of(EventType type, String targetId, T payload) {
        return new ChatEvent<>(type, targetId, payload);
    }
}
