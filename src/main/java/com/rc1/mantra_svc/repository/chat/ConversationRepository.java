package com.rc1.mantra_svc.repository.chat;

import com.rc1.mantra_svc.model.chat.Conversation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive repository for direct (1-to-1) conversations.
 */
public interface ConversationRepository extends ReactiveMongoRepository<Conversation, String> {

    /**
     * Find a conversation that contains exactly these two participant IDs.
     */
    @Query("{ 'participantIds': { $all: ?0, $size: 2 } }")
    Mono<Conversation> findByParticipants(List<String> participantIds);

    /**
     * All conversations for a given user, newest first.
     */
    @Query(value = "{ 'participantIds': ?0 }", sort = "{ 'updatedAt': -1 }")
    Flux<Conversation> findByParticipantId(String userId);
}
