package com.rc1.mantra_svc.repository;

import com.rc1.mantra_svc.model.Expense;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive MongoDB repository for {@link Expense}. */
public interface ExpenseRepository extends ReactiveMongoRepository<Expense, String> {

    Flux<Expense> findByGroupIdOrderByExpenseDateDesc(String groupId);

    Flux<Expense> findByGroupIdAndSettledFalse(String groupId);

    Mono<Void> deleteByGroupId(String groupId);
}
