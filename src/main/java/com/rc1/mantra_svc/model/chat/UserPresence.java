package com.rc1.mantra_svc.model.chat;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Tracks per-user online/offline presence.
 * Upserted on WebSocket connect/disconnect.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "presence")
public class UserPresence {

    @Id
    private String userId;

    private boolean online;

    private Instant lastSeenAt;

    /** WebSocket session ID — allows detecting stale records. */
    private String sessionId;

    @LastModifiedDate
    private Instant updatedAt;
}
