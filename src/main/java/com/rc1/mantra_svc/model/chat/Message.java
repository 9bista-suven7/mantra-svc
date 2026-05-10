package com.rc1.mantra_svc.model.chat;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A single chat message in either a direct conversation or a group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
@CompoundIndexes({
    @CompoundIndex(name = "conv_time_idx", def = "{'conversationId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "group_time_idx", def = "{'groupId': 1, 'createdAt': -1}")
})
public class Message {

    @Id
    private String id;

    /** Set for direct (1-to-1) messages. */
    @Indexed
    private String conversationId;

    /** Set for group messages. */
    @Indexed
    private String groupId;

    private String senderId;

    private MessageType type;

    /** Text content — null for FILE/IMAGE types. */
    private String content;

    /** Attachment metadata — populated for IMAGE/FILE types. */
    private Attachment attachment;

    /** Per-user read receipts. */
    @Builder.Default
    private List<ReadReceipt> readReceipts = new ArrayList<>();

    /**
     * Overall status computed as: SENT -> DELIVERED (when all delivered)
     * -> READ (when at least one recipient has read it).
     */
    private MessageStatus status;

    /** ID of message being replied to. */
    private String replyToMessageId;

    /** True if the sender deleted this message. */
    @Builder.Default
    private boolean deleted = false;

    /** Replacement content after edit. */
    private String editedContent;

    private Instant editedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /** Records which user read the message and when. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadReceipt {
        private String userId;
        private Instant readAt;
    }

    /** File/image metadata embedded in the message. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        private String fileId;
        private String originalName;
        private String contentType;
        private long sizeBytes;
        private String url;
        /** For images: pre-generated thumbnail URL. */
        private String thumbnailUrl;
    }
}
