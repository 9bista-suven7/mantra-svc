package com.rc1.mantra_svc.repository;

import com.rc1.mantra_svc.model.Note;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/** Reactive MongoDB repository for {@link Note}. */
public interface NoteRepository extends ReactiveMongoRepository<Note, String> {

    Flux<Note> findByUserIdAndArchivedFalseOrderByPinnedDescUpdatedAtDesc(String userId);

    Flux<Note> findByUserIdAndArchivedTrue(String userId);

    Flux<Note> findByUserIdAndTagsContaining(String userId, String tag);
}
