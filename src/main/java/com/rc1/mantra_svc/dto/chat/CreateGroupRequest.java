package com.rc1.mantra_svc.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request to create a new group chat.
 */
@Data
public class CreateGroupRequest {
    @NotBlank
    @Size(min = 2, max = 50)
    private String name;

    @Size(max = 200)
    private String description;

    /** Initial member user IDs (creator is added automatically as ADMIN). */
    private List<String> memberIds;

    /** Optional — set when this group is being created from a Splitwise expense group. */
    private String linkedExpenseGroupId;
}
