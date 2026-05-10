package com.rc1.mantra_svc.repository.chat;

import com.rc1.mantra_svc.model.chat.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Reactive repository for chat messages.
 */
public interface MessageRepository extends ReactiveMongoRepository<Message, String> {

    /** Paginated message history for a direct conversation (newest first). */
    Flux<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    /** Paginated message history for a group (newest first). */
    Flux<Message> findByGroupIdOrderByCreatedAtDesc(String groupId, Pageable pageable);

    /** Messages after a cursor timestamp (for load-more). */
    Flux<Message> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
        String conversationId, Instant before, Pageable pageable);

    Flux<Message> findByGroupIdAndCreatedAtBeforeOrderByCreatedAtDesc(
        String groupId, Instant before, Pageable pageable);

    /** Unread count for a user in a direct conversation. */
    Mono<Long> countByConversationIdAndSenderIdNotAndReadReceiptsUserIdNot(
        String conversationId, String senderId, String userId);
}
