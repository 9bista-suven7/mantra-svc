package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.dto.ApiResponse;
import com.rc1.mantra_svc.dto.todo.TodoRequest;
import com.rc1.mantra_svc.model.Todo;
import com.rc1.mantra_svc.security.UserPrincipal;
import com.rc1.mantra_svc.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Todo CRUD REST API — {@code /api/todos}.
 */
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<Todo>>> create(@Valid @RequestBody TodoRequest request) {
        return currentUserId()
                .flatMap(uid -> todoService.create(uid, request))
                .map(t -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(t)));
    }

    @GetMapping
    public Mono<List<Todo>> getAll(@RequestParam(required = false) String status) {
        return currentUserId().flatMapMany(uid -> {
            if (status != null) {
                return todoService.getByStatus(uid, Todo.TodoStatus.valueOf(status.toUpperCase()));
            }
            return todoService.getAll(uid);
        }).collectList();
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Todo>>> update(
            @PathVariable String id, @Valid @RequestBody TodoRequest request) {
        return currentUserId()
                .flatMap(uid -> todoService.update(id, uid, request))
                .map(t -> ResponseEntity.ok(ApiResponse.success(t)));
    }

    @PatchMapping("/{id}/complete")
    public Mono<ResponseEntity<ApiResponse<Todo>>> complete(@PathVariable String id) {
        return currentUserId()
                .flatMap(uid -> todoService.complete(id, uid))
                .map(t -> ResponseEntity.ok(ApiResponse.success(t)));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return currentUserId()
                .flatMap(uid -> todoService.delete(id, uid))
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    // -----------------------------------------------------------------------

    private Mono<String> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ((UserPrincipal) ctx.getAuthentication().getPrincipal()).getUserId());
    }
}
