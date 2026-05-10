package com.rc1.mantra_svc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * A named group of people who share expenses (e.g. "Goa Trip 2026", "Flat").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "expense_groups")
public class ExpenseGroup {

    @Id
    private String id;

    private String name;
    private String description;

    /** Emoji icon displayed on the group card (e.g. "✈️"). */
    private String emoji;

    /** Group category: TRIP, HOME, FOOD, WORK, OTHER. */
    private String category;

    @Indexed
    private String createdById;

    private List<String> memberIds;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Builder.Default
    private boolean active = true;
}
