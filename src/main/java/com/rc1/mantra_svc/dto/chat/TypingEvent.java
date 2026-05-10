package com.rc1.mantra_svc.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Typing indicator payload. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TypingEvent {
    private String userId;
    private String displayName;
    private String targetId;   // conversationId or groupId
    private boolean typing;
}
