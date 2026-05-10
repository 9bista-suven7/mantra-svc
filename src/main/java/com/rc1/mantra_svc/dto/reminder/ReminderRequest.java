package com.rc1.mantra_svc.dto.reminder;

import com.rc1.mantra_svc.model.Reminder;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/** Create / update request for a reminder. */
@Data
public class ReminderRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Reminder time is required")
    @FutureOrPresent(message = "Reminder time must not be in the past")
    private Instant reminderTime;

    private Reminder.RecurrenceType recurrence = Reminder.RecurrenceType.NONE;
    private Reminder.Priority priority = Reminder.Priority.MEDIUM;
    private List<String> tags;
}
