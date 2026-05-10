package com.rc1.mantra_svc.repository.chat;

import com.rc1.mantra_svc.model.chat.Group;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for group chats.
 */
public interface GroupRepository extends ReactiveMongoRepository<Group, String> {

    /** All non-archived groups the user is an active member of. */
    @Query(value = "{ 'members': { $elemMatch: { 'userId': ?0, 'leftAt': null } }, 'archived': false }",
           sort = "{ 'updatedAt': -1 }")
    Flux<Group> findActiveGroupsByUserId(String userId);

    /** Find a chat group linked to a given Splitwise expense group, if one exists. */
    Mono<Group> findFirstByLinkedExpenseGroupId(String linkedExpenseGroupId);
}
