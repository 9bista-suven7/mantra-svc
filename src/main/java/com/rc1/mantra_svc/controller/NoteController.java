package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.dto.ApiResponse;
import com.rc1.mantra_svc.dto.note.NoteRequest;
import com.rc1.mantra_svc.model.Note;
import com.rc1.mantra_svc.security.UserPrincipal;
import com.rc1.mantra_svc.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Notes CRUD REST API.
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<Note>>> create(@Valid @RequestBody NoteRequest request) {
        return currentUserId()
                .flatMap(uid -> noteService.createNote(uid, request))
                .map(n -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(n)));
    }

    @GetMapping
    public Mono<List<Note>> getAll() {
        return currentUserId()
                .flatMapMany(noteService::getUserNotes)
                .collectList();
    }

    @GetMapping("/archived")
    public Mono<List<Note>> getArchived() {
        return currentUserId()
                .flatMapMany(noteService::getArchivedNotes)
                .collectList();
    }

    @GetMapping("/tag/{tag}")
    public Mono<List<Note>> getByTag(@PathVariable String tag) {
        return currentUserId()
                .flatMapMany(uid -> noteService.getNotesByTag(uid, tag))
                .collectList();
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Note>>> update(
            @PathVariable String id, @Valid @RequestBody NoteRequest request) {
        return currentUserId()
                .flatMap(uid -> noteService.updateNote(id, uid, request))
                .map(n -> ResponseEntity.ok(ApiResponse.success(n)));
    }

    @PatchMapping("/{id}/pin")
    public Mono<ResponseEntity<ApiResponse<Note>>> togglePin(@PathVariable String id) {
        return currentUserId()
                .flatMap(uid -> noteService.togglePin(id, uid))
                .map(n -> ResponseEntity.ok(ApiResponse.success(n)));
    }

    @PatchMapping("/{id}/archive")
    public Mono<ResponseEntity<ApiResponse<Note>>> toggleArchive(@PathVariable String id) {
        return currentUserId()
                .flatMap(uid -> noteService.toggleArchive(id, uid))
                .map(n -> ResponseEntity.ok(ApiResponse.success(n)));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return currentUserId()
                .flatMap(uid -> noteService.deleteNote(id, uid))
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    private Mono<String> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ((UserPrincipal) ctx.getAuthentication().getPrincipal()).getUserId());
    }
}
