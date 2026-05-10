package com.rc1.mantra_svc.repository;

import com.rc1.mantra_svc.model.Todo;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/** Reactive MongoDB repository for {@link Todo}. */
public interface TodoRepository extends ReactiveMongoRepository<Todo, String> {

    Flux<Todo> findByUserIdOrderByCreatedAtDesc(String userId);

    Flux<Todo> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, Todo.TodoStatus status);
}
