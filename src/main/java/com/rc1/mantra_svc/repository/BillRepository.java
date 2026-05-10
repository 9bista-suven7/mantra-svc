package com.rc1.mantra_svc.repository;

import com.rc1.mantra_svc.model.Bill;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

/** Reactive MongoDB repository for {@link Bill}. */
public interface BillRepository extends ReactiveMongoRepository<Bill, String> {

    Flux<Bill> findByUserIdOrderByDueDateAsc(String userId);

    Flux<Bill> findByUserIdAndStatus(String userId, Bill.BillStatus status);

    Flux<Bill> findByUserIdAndDueDateBetween(String userId, Instant from, Instant to);

    Flux<Bill> findByUserIdAndCategory(String userId, Bill.BillCategory category);
}
