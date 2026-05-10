package com.rc1.mantra_svc.dto.chat;

import com.rc1.mantra_svc.model.chat.MessageStatus;
import com.rc1.mantra_svc.model.chat.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Outbound DTO returned to clients for a single message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private String id;
    private String conversationId;
    private String groupId;
    private String senderId;
    private String senderDisplayName;
    private String senderAvatarColor;
    private MessageType type;
    private String content;
    private AttachmentDto attachment;
    private List<ReadReceiptDto> readReceipts;
    private MessageStatus status;
    private String replyToMessageId;
    private String replyToContent;
    private boolean deleted;
    private String editedContent;
    private Instant editedAt;
    private Instant createdAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AttachmentDto {
        private String fileId;
        private String originalName;
        private String contentType;
        private long sizeBytes;
        private String url;
        private String thumbnailUrl;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReadReceiptDto {
        private String userId;
        private String displayName;
        private Instant readAt;
    }
}
