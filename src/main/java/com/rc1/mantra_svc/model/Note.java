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
 * A rich-text note with colour coding, tags, and pin support.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notes")
public class Note {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;

    /** Markdown or plain-text note body. */
    private String content;

    private List<String> tags;

    /** Background hex colour for the card (e.g. "#1e1e2e"). */
    @Builder.Default
    private String color = "#1e1e2e";

    @Builder.Default
    private boolean pinned = false;

    @Builder.Default
    private boolean archived = false;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
