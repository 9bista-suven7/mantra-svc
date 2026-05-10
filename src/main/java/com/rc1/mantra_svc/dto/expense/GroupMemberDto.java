package com.rc1.mantra_svc.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lightweight member info returned for the group members list. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDto {
    private String userId;
    private String displayName;
    private String email;
    private String initials;
}
