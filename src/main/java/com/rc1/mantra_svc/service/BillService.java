package com.rc1.mantra_svc.service;

import com.rc1.mantra_svc.dto.bill.BillRequest;
import com.rc1.mantra_svc.exception.ResourceNotFoundException;
import com.rc1.mantra_svc.service.chat.FileStorageService;
import org.springframework.http.codec.multipart.FilePart;
import com.rc1.mantra_svc.model.Bill;
import com.rc1.mantra_svc.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * CRUD operations for bills and documents with due-date utilities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final FileStorageService fileStorageService;

    /** Creates a bill for the given user. */
    public Mono<Bill> createBill(String userId, BillRequest request) {
        Bill bill = Bill.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .dueDate(request.getDueDate())
                .status(Bill.BillStatus.UNPAID)
                .recurring(request.isRecurring())
                .recurrencePattern(request.getRecurrencePattern())
                .tags(request.getTags())
                .build();
        return billRepository.save(bill);
    }

    /** Returns all bills for a user ordered by due date ascending. */
    public Flux<Bill> getUserBills(String userId) {
        return billRepository.findByUserIdOrderByDueDateAsc(userId);
    }

    /** Returns bills due within the next 30 days. */
    public Flux<Bill> getUpcomingBills(String userId) {
        Instant now = Instant.now();
        Instant in30Days = now.plus(30, ChronoUnit.DAYS);
        return billRepository.findByUserIdAndDueDateBetween(userId, now, in30Days);
    }

    /** Returns bills filtered by status. */
    public Flux<Bill> getBillsByStatus(String userId, Bill.BillStatus status) {
        return billRepository.findByUserIdAndStatus(userId, status);
    }

    /** Returns bills filtered by category. */
    public Flux<Bill> getBillsByCategory(String userId, Bill.BillCategory category) {
        return billRepository.findByUserIdAndCategory(userId, category);
    }

    /** Updates a bill owned by the given user. */
    public Mono<Bill> updateBill(String id, String userId, BillRequest request) {
        return getOwnedBill(id, userId)
                .flatMap(existing -> {
                    existing.setTitle(request.getTitle());
                    existing.setDescription(request.getDescription());
                    existing.setCategory(request.getCategory());
                    existing.setAmount(request.getAmount());
                    if (request.getCurrency() != null) existing.setCurrency(request.getCurrency());
                    existing.setDueDate(request.getDueDate());
                    existing.setRecurring(request.isRecurring());
                    existing.setRecurrencePattern(request.getRecurrencePattern());
                    existing.setTags(request.getTags());
                    return billRepository.save(existing);
                });
    }

    /** Marks a bill as PAID. */
    public Mono<Bill> markPaid(String id, String userId) {
        return getOwnedBill(id, userId)
                .flatMap(bill -> {
                    bill.setStatus(Bill.BillStatus.PAID);
                    return billRepository.save(bill);
                });
    }

    /** Permanently deletes a bill owned by the given user. */
    public Mono<Void> deleteBill(String id, String userId) {
        return getOwnedBill(id, userId)
                .flatMap(billRepository::delete);
    }

    /** Attaches a receipt/photo to a bill (replaces any existing file). */
    public Mono<Bill> uploadReceipt(String id, String userId, FilePart filePart) {
        return getOwnedBill(id, userId)
                .flatMap(bill -> fileStorageService.store(filePart)
                        .flatMap(uploaded -> {
                            bill.setFileUrl(uploaded.getUrl());
                            bill.setFileName(uploaded.getOriginalName());
                            bill.setFileType(uploaded.getContentType());
                            return billRepository.save(bill);
                        }));
    }

    /** Removes the receipt attachment from a bill. */
    public Mono<Bill> deleteReceipt(String id, String userId) {
        return getOwnedBill(id, userId)
                .flatMap(bill -> {
                    bill.setFileUrl(null);
                    bill.setFileName(null);
                    bill.setFileType(null);
                    return billRepository.save(bill);
                });
    }

    private Mono<Bill> getOwnedBill(String id, String userId) {
        return billRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Bill not found")))
                .flatMap(bill -> {
                    if (!bill.getUserId().equals(userId)) {
                        return Mono.error(new ResourceNotFoundException("Bill not found"));
                    }
                    return Mono.just(bill);
                });
    }
}
