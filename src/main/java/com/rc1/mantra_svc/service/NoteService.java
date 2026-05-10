package com.rc1.mantra_svc.service;

import com.rc1.mantra_svc.dto.note.NoteRequest;
import com.rc1.mantra_svc.exception.ResourceNotFoundException;
import com.rc1.mantra_svc.model.Note;
import com.rc1.mantra_svc.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * CRUD operations for notes, including pin/archive/search.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;

    /** Creates a note for the given user. */
    public Mono<Note> createNote(String userId, NoteRequest request) {
        Note note = Note.builder()
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .tags(request.getTags())
                .color(request.getColor() != null ? request.getColor() : "#1e1e2e")
                .pinned(false)
                .archived(false)
                .build();
        return noteRepository.save(note);
    }

    /** Returns active (non-archived) notes — pinned notes first. */
    public Flux<Note> getUserNotes(String userId) {
        return noteRepository.findByUserIdAndArchivedFalseOrderByPinnedDescUpdatedAtDesc(userId);
    }

    /** Returns all archived notes for the user. */
    public Flux<Note> getArchivedNotes(String userId) {
        return noteRepository.findByUserIdAndArchivedTrue(userId);
    }

    /** Returns notes matching a specific tag. */
    public Flux<Note> getNotesByTag(String userId, String tag) {
        return noteRepository.findByUserIdAndTagsContaining(userId, tag);
    }

    /** Updates a note owned by the given user. */
    public Mono<Note> updateNote(String id, String userId, NoteRequest request) {
        return getOwnedNote(id, userId)
                .flatMap(existing -> {
                    existing.setTitle(request.getTitle());
                    existing.setContent(request.getContent());
                    existing.setTags(request.getTags());
                    if (request.getColor() != null) existing.setColor(request.getColor());
                    return noteRepository.save(existing);
                });
    }

    /** Toggles the pinned state of a note. */
    public Mono<Note> togglePin(String id, String userId) {
        return getOwnedNote(id, userId)
                .flatMap(note -> {
                    note.setPinned(!note.isPinned());
                    return noteRepository.save(note);
                });
    }

    /** Toggles the archived state of a note. */
    public Mono<Note> toggleArchive(String id, String userId) {
        return getOwnedNote(id, userId)
                .flatMap(note -> {
                    note.setArchived(!note.isArchived());
                    return noteRepository.save(note);
                });
    }

    /** Permanently deletes a note owned by the given user. */
    public Mono<Void> deleteNote(String id, String userId) {
        return getOwnedNote(id, userId)
                .flatMap(noteRepository::delete);
    }

    private Mono<Note> getOwnedNote(String id, String userId) {
        return noteRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Note not found")))
                .flatMap(note -> {
                    if (!note.getUserId().equals(userId)) {
                        return Mono.error(new ResourceNotFoundException("Note not found"));
                    }
                    return Mono.just(note);
                });
    }
}
