package com.rc1.mantra_svc.service;

import com.rc1.mantra_svc.dto.todo.TodoRequest;
import com.rc1.mantra_svc.exception.ResourceNotFoundException;
import com.rc1.mantra_svc.model.Todo;
import com.rc1.mantra_svc.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * CRUD operations for user todo items.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository todoRepository;

    /** Creates a new todo for the given user. */
    public Mono<Todo> create(String userId, TodoRequest request) {
        Todo todo = Todo.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Todo.Priority.MEDIUM)
                .dueDate(request.getDueDate())
                .tags(request.getTags())
                .status(Todo.TodoStatus.TODO)
                .build();
        return todoRepository.save(todo)
                .doOnSuccess(t -> log.debug("Created todo [{}] for user [{}]", t.getId(), userId));
    }

    /** Returns all todos for a user, newest first. */
    public Flux<Todo> getAll(String userId) {
        return todoRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Returns todos for a user filtered by status. */
    public Flux<Todo> getByStatus(String userId, Todo.TodoStatus status) {
        return todoRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
    }

    /** Updates a todo owned by the given user. */
    public Mono<Todo> update(String id, String userId, TodoRequest request) {
        return getOwned(id, userId)
                .flatMap(existing -> {
                    existing.setTitle(request.getTitle());
                    existing.setDescription(request.getDescription());
                    if (request.getPriority() != null) existing.setPriority(request.getPriority());
                    if (request.getDueDate() != null)  existing.setDueDate(request.getDueDate());
                    if (request.getTags() != null)     existing.setTags(request.getTags());
                    if (request.getStatus() != null)   existing.setStatus(request.getStatus());
                    existing.setUpdatedAt(Instant.now());
                    return todoRepository.save(existing);
                });
    }

    /** Marks a todo as DONE. */
    public Mono<Todo> complete(String id, String userId) {
        return getOwned(id, userId)
                .flatMap(existing -> {
                    existing.setStatus(Todo.TodoStatus.DONE);
                    existing.setUpdatedAt(Instant.now());
                    return todoRepository.save(existing);
                });
    }

    /** Deletes a todo owned by the given user. */
    public Mono<Void> delete(String id, String userId) {
        return getOwned(id, userId)
                .flatMap(todoRepository::delete);
    }

    // -----------------------------------------------------------------------

    private Mono<Todo> getOwned(String id, String userId) {
        return todoRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Todo not found: " + id)))
                .filter(t -> t.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Todo not found: " + id)));
    }
}
