package com.rc1.mantra_svc.dto.chat;

import com.rc1.mantra_svc.model.chat.Group;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Outbound DTO for a group chat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDto {
    private String id;
    private String name;
    private String description;
    private String avatarUrl;
    private String createdBy;
    /** Splitwise expense-group id this chat is linked to (nullable). */
    private String linkedExpenseGroupId;
    private List<GroupMemberDto> members;
    private com.rc1.mantra_svc.model.chat.Conversation.MessageSummary lastMessage;
    private int unreadCount;
    private Instant updatedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class GroupMemberDto {
        private String userId;
        private String displayName;
        private String username;
        private String avatarColor;
        private Group.GroupRole role;
        private boolean online;
        private Instant joinedAt;
    }
}
