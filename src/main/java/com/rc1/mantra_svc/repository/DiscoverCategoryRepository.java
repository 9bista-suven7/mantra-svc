package com.rc1.mantra_svc.repository;

import com.rc1.mantra_svc.model.DiscoverCategory;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/** Reactive MongoDB repository for {@link DiscoverCategory}. */
public interface DiscoverCategoryRepository extends ReactiveMongoRepository<DiscoverCategory, String> {

    Flux<DiscoverCategory> findAllByOrderBySortOrderAsc();
}
