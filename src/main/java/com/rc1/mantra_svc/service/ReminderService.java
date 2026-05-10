package com.rc1.mantra_svc.service;

import com.rc1.mantra_svc.dto.reminder.ReminderRequest;
import com.rc1.mantra_svc.exception.ResourceNotFoundException;
import com.rc1.mantra_svc.model.Reminder;
import com.rc1.mantra_svc.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;


/**
 * CRUD operations for reminders plus a scheduled job to mark overdue reminders.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;

    /** Creates a new reminder for the given user. */
    public Mono<Reminder> createReminder(String userId, ReminderRequest request) {
        Reminder reminder = Reminder.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .reminderTime(request.getReminderTime())
                .recurrence(request.getRecurrence() != null
                        ? request.getRecurrence() : Reminder.RecurrenceType.NONE)
                .priority(request.getPriority() != null
                        ? request.getPriority() : Reminder.Priority.MEDIUM)
                .tags(request.getTags())
                .status(Reminder.ReminderStatus.PENDING)
                .notified(false)
                .build();
        return reminderRepository.save(reminder);
    }

    /** Returns all reminders for a user ordered by time ascending. */
    public Flux<Reminder> getUserReminders(String userId) {
        return reminderRepository.findByUserIdOrderByReminderTimeAsc(userId);
    }

    /** Returns upcoming (PENDING) reminders within the next 7 days. */
    public Flux<Reminder> getUpcomingReminders(String userId) {
        return reminderRepository.findByUserIdAndStatusOrderByReminderTimeAsc(
                userId, Reminder.ReminderStatus.PENDING);
    }

    /** Updates a reminder owned by the given user. */
    public Mono<Reminder> updateReminder(String id, String userId, ReminderRequest request) {
        return getOwnedReminder(id, userId)
                .flatMap(existing -> {
                    existing.setTitle(request.getTitle());
                    existing.setDescription(request.getDescription());
                    existing.setReminderTime(request.getReminderTime());
                    existing.setRecurrence(request.getRecurrence());
                    existing.setPriority(request.getPriority());
                    existing.setTags(request.getTags());
                    return reminderRepository.save(existing);
                });
    }

    /** Marks a reminder as COMPLETED. */
    public Mono<Reminder> completeReminder(String id, String userId) {
        return getOwnedReminder(id, userId)
                .flatMap(reminder -> {
                    reminder.setStatus(Reminder.ReminderStatus.COMPLETED);
                    return reminderRepository.save(reminder);
                });
    }

    /** Deletes a reminder owned by the given user. */
    public Mono<Void> deleteReminder(String id, String userId) {
        return getOwnedReminder(id, userId)
                .flatMap(reminderRepository::delete);
    }

    /**
     * Scheduled job: every minute, mark overdue reminders as notified.
     * In production, extend this to push a notification via WebSocket / FCM.
     */
    @Scheduled(fixedRateString = "${app.reminder.check-interval-ms:60000}")
    public void processOverdueReminders() {
        reminderRepository.findByStatusAndNotifiedFalseAndReminderTimeBefore(
                        Reminder.ReminderStatus.PENDING, Instant.now())
                .flatMap(reminder -> {
                    log.info("Reminder due for userId={}: {}", reminder.getUserId(), reminder.getTitle());
                    reminder.setNotified(true);
                    return reminderRepository.save(reminder);
                })
                .subscribe();
    }

    private Mono<Reminder> getOwnedReminder(String id, String userId) {
        return reminderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Reminder not found")))
                .flatMap(r -> {
                    if (!r.getUserId().equals(userId)) {
                        return Mono.error(new ResourceNotFoundException("Reminder not found"));
                    }
                    return Mono.just(r);
                });
    }
}
