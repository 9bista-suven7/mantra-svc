package com.rc1.mantra_svc.dto.chat;

import com.rc1.mantra_svc.model.chat.Conversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Outbound DTO for a direct (1-to-1) conversation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {
    private String id;
    /** The other participant (not the requesting user). */
    private ParticipantDto otherUser;
    private Conversation.MessageSummary lastMessage;
    private int unreadCount;
    private boolean otherUserOnline;
    private Instant lastSeenAt;
    private Instant updatedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParticipantDto {
        private String id;
        private String displayName;
        private String username;
        private String avatarColor;
    }
}
