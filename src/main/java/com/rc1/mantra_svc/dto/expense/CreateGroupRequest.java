package com.rc1.mantra_svc.dto.expense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** Request to create a new expense group. */
@Data
public class CreateGroupRequest {

    @NotBlank(message = "Group name is required")
    @Size(min = 2, max = 60, message = "Group name must be 2–60 characters")
    private String name;

    private String description;

    /** Emoji e.g. "✈️", "🏠", "🍔" */
    private String emoji;

    /** TRIP | HOME | FOOD | WORK | OTHER */
    private String category;

    /** Email addresses of members to invite (excluding creator). */
    private List<String> memberEmails;
}
