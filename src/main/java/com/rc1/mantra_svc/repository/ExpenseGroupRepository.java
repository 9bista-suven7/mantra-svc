package com.rc1.mantra_svc.repository;

import com.rc1.mantra_svc.model.ExpenseGroup;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/** Reactive MongoDB repository for {@link ExpenseGroup}. */
public interface ExpenseGroupRepository extends ReactiveMongoRepository<ExpenseGroup, String> {

    Flux<ExpenseGroup> findByMemberIdsContainingAndActiveTrue(String userId);

    Flux<ExpenseGroup> findByCreatedByIdAndActiveTrue(String createdById);
}
