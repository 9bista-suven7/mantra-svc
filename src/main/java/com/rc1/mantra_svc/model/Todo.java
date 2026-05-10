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
 * A user todo item with optional priority, due-date, and tags.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "todos")
public class Todo {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;
    private String description;
    private Priority priority;
    private Instant dueDate;
    private List<String> tags;

    @Builder.Default
    private TodoStatus status = TodoStatus.TODO;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }

    public enum TodoStatus {
        TODO, IN_PROGRESS, DONE, CANCELLED
    }
}
