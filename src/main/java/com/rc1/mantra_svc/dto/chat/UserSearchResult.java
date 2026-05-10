package com.rc1.mantra_svc.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lightweight user projection returned by the people-search endpoint. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResult {
    private String id;
    private String email;
    private String username;
    private String displayName;
    private String avatarColor;
}
