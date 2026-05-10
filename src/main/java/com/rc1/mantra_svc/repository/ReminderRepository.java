package com.rc1.mantra_svc.repository;

import com.rc1.mantra_svc.model.Reminder;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

/** Reactive MongoDB repository for {@link Reminder}. */
public interface ReminderRepository extends ReactiveMongoRepository<Reminder, String> {

    Flux<Reminder> findByUserIdOrderByReminderTimeAsc(String userId);

    Flux<Reminder> findByUserIdAndStatusOrderByReminderTimeAsc(String userId, Reminder.ReminderStatus status);

    /** Used by the scheduler to find pending, unnotified reminders due by a given time. */
    Flux<Reminder> findByStatusAndNotifiedFalseAndReminderTimeBefore(
            Reminder.ReminderStatus status, Instant time);
}
