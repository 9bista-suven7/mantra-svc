package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.dto.ApiResponse;
import com.rc1.mantra_svc.dto.reminder.ReminderRequest;
import com.rc1.mantra_svc.model.Reminder;
import com.rc1.mantra_svc.security.UserPrincipal;
import com.rc1.mantra_svc.service.ReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reminder CRUD REST API.
 */
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<Reminder>>> create(
            @Valid @RequestBody ReminderRequest request) {
        return currentUserId()
                .flatMap(uid -> reminderService.createReminder(uid, request))
                .map(r -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(r)));
    }

    @GetMapping
    public Mono<List<Reminder>> getAll() {
        return currentUserId()
                .flatMapMany(reminderService::getUserReminders)
                .collectList();
    }

    @GetMapping("/upcoming")
    public Mono<List<Reminder>> getUpcoming() {
        return currentUserId()
                .flatMapMany(reminderService::getUpcomingReminders)
                .collectList();
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Reminder>>> update(
            @PathVariable String id, @Valid @RequestBody ReminderRequest request) {
        return currentUserId()
                .flatMap(uid -> reminderService.updateReminder(id, uid, request))
                .map(r -> ResponseEntity.ok(ApiResponse.success(r)));
    }

    @PatchMapping("/{id}/complete")
    public Mono<ResponseEntity<ApiResponse<Reminder>>> complete(@PathVariable String id) {
        return currentUserId()
                .flatMap(uid -> reminderService.completeReminder(id, uid))
                .map(r -> ResponseEntity.ok(ApiResponse.success(r)));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return currentUserId()
                .flatMap(uid -> reminderService.deleteReminder(id, uid))
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    private Mono<String> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ((UserPrincipal) ctx.getAuthentication().getPrincipal()).getUserId());
    }
}
