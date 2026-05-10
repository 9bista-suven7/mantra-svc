package com.rc1.mantra_svc.dto.todo;

import com.rc1.mantra_svc.model.Todo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Request body for creating or updating a {@link Todo}.
 */
@Data
public class TodoRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Todo.Priority priority;

    private Instant dueDate;

    private List<String> tags;

    private Todo.TodoStatus status;
}
