package com.rc1.mantra_svc.repository.chat;

import com.rc1.mantra_svc.model.chat.UserPresence;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Reactive repository for user presence tracking.
 */
public interface PresenceRepository extends ReactiveMongoRepository<UserPresence, String> {

    Flux<UserPresence> findByUserIdIn(List<String> userIds);
}
