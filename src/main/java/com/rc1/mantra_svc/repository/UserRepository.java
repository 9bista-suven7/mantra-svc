package com.rc1.mantra_svc.repository;

import com.rc1.mantra_svc.model.User;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive MongoDB repository for {@link User}. */
public interface UserRepository extends ReactiveMongoRepository<User, String> {

    Mono<User> findByEmail(String email);

    Mono<User> findByUsername(String username);

    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByUsername(String username);

    /**
     * Full-text-style search across displayName, email, and username.
     * Uses case-insensitive regex so callers need only pass the raw query string.
     */
    @Query("{ '$or': [ " +
           "  { 'displayName': { '$regex': ?0, '$options': 'i' } }, " +
           "  { 'email':       { '$regex': ?0, '$options': 'i' } }, " +
           "  { 'username':    { '$regex': ?0, '$options': 'i' } } " +
           "] }")
    Flux<User> searchUsers(String query);
}

