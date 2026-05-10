package com.rc1.mantra_svc.dto.chat;

import com.rc1.mantra_svc.model.chat.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound DTO for sending a new message via REST or WebSocket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    /** One of conversationId or groupId must be set. */
    private String conversationId;
    private String groupId;

    @NotNull
    private MessageType type;

    /** Required for TEXT/EMOJI types. */
    private String content;

    /** Required for FILE/IMAGE types (returned by file upload endpoint). */
    private String fileId;

    /** ID of message being replied to (optional). */
    private String replyToMessageId;
}
