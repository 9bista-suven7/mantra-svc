package com.rc1.mantra_svc.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Presence update payload broadcast to subscribers. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PresenceEvent {
    private String userId;
    private String displayName;
    private boolean online;
    private Instant lastSeenAt;
}
