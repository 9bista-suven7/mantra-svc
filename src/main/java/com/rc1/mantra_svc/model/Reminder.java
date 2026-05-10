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
 * A user reminder with optional recurrence and priority.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reminders")
public class Reminder {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;
    private String description;
    private Instant reminderTime;
    private RecurrenceType recurrence;
    private List<String> tags;
    private Priority priority;

    @Builder.Default
    private ReminderStatus status = ReminderStatus.PENDING;

    @Builder.Default
    private boolean notified = false;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum RecurrenceType {
        NONE, DAILY, WEEKLY, MONTHLY
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }

    public enum ReminderStatus {
        PENDING, COMPLETED, SNOOZED, CANCELLED
    }
}
