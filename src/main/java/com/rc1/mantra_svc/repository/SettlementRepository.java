package com.rc1.mantra_svc.repository;

import com.rc1.mantra_svc.model.Settlement;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive MongoDB repository for {@link Settlement}. */
public interface SettlementRepository extends ReactiveMongoRepository<Settlement, String> {

    Flux<Settlement> findByGroupIdOrderBySettledAtDesc(String groupId);

    Flux<Settlement> findByFromUserIdOrToUserId(String fromUserId, String toUserId);

    Mono<Void> deleteByGroupId(String groupId);
}
