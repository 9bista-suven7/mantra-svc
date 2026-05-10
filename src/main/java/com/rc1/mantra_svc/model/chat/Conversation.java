package com.rc1.mantra_svc.model.chat;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Represents a direct (1-to-1) conversation between two users.
 * For group chats use {@link Group}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations")
@CompoundIndexes({
    @CompoundIndex(name = "participants_idx", def = "{'participantIds': 1}")
})
public class Conversation {

    @Id
    private String id;

    /** IDs of exactly two participants (sorted lexicographically on creation). */
    private List<String> participantIds;

    /** Snapshot of the last message for sidebar display. */
    private MessageSummary lastMessage;

    /** Per-participant unread count: key = userId, value = count. */
    private java.util.Map<String, Integer> unreadCounts;

    /**
     * Participant IDs who chose to hide this conversation from their list.
     * The conversation is automatically un-hidden for a user when a new
     * message arrives for them.
     */
    @Builder.Default
    private java.util.Set<String> hiddenFor = new java.util.HashSet<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /** Lightweight summary embedded in conversation. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageSummary {
        private String messageId;
        private String senderId;
        private String content;
        private MessageType type;
        private Instant sentAt;
    }
}
