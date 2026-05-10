package com.rc1.mantra_svc.model.chat;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A group chat with multiple members and admin roles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "groups")
public class Group {

    @Id
    private String id;

    private String name;

    private String description;

    /** URL to group avatar image. */
    private String avatarUrl;

    /** User ID of the original creator. */
    private String createdBy;

    /**
     * If this chat group was auto-created from a Splitwise expense group, this
     * holds that expense group's id. Used by the UI to display the chat under
     * the "Splitwise Groups" section instead of "My Groups" — visible to every
     * member regardless of which account opens the app.
     */
    private String linkedExpenseGroupId;

    /** List of all members including admins. */
    @Builder.Default
    private List<GroupMember> members = new ArrayList<>();

    /** Snapshot of the last message for sidebar display. */
    private Conversation.MessageSummary lastMessage;

    /** Per-member unread count: key = userId, value = count. */
    @Builder.Default
    private java.util.Map<String, Integer> unreadCounts = new java.util.HashMap<>();

    @Builder.Default
    private boolean archived = false;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /** Embedded member record. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupMember {
        private String userId;
        private GroupRole role;
        private Instant joinedAt;
        /** Set when the member leaves without being removed. */
        private Instant leftAt;
        /** Muted until this time (null = not muted). */
        private Instant mutedUntil;
    }

    public enum GroupRole {
        ADMIN,
        MEMBER
    }
}
