package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.dto.ApiResponse;
import com.rc1.mantra_svc.dto.bill.BillRequest;
import com.rc1.mantra_svc.model.Bill;
import com.rc1.mantra_svc.security.UserPrincipal;
import com.rc1.mantra_svc.service.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Bills and documents REST API.
 */
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<Bill>>> create(@Valid @RequestBody BillRequest request) {
        return currentUserId()
                .flatMap(uid -> billService.createBill(uid, request))
                .map(b -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(b)));
    }

    @GetMapping
    public Mono<List<Bill>> getAll() {
        return currentUserId()
                .flatMapMany(billService::getUserBills)
                .collectList();
    }

    @GetMapping("/upcoming")
    public Mono<List<Bill>> getUpcoming() {
        return currentUserId()
                .flatMapMany(billService::getUpcomingBills)
                .collectList();
    }

    @GetMapping("/status/{status}")
    public Mono<List<Bill>> getByStatus(@PathVariable Bill.BillStatus status) {
        return currentUserId()
                .flatMapMany(uid -> billService.getBillsByStatus(uid, status))
                .collectList();
    }

    @GetMapping("/category/{category}")
    public Mono<List<Bill>> getByCategory(@PathVariable Bill.BillCategory category) {
        return currentUserId()
                .flatMapMany(uid -> billService.getBillsByCategory(uid, category))
                .collectList();
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Bill>>> update(
            @PathVariable String id, @Valid @RequestBody BillRequest request) {
        return currentUserId()
                .flatMap(uid -> billService.updateBill(id, uid, request))
                .map(b -> ResponseEntity.ok(ApiResponse.success(b)));
    }

    @PatchMapping("/{id}/pay")
    public Mono<ResponseEntity<ApiResponse<Bill>>> markPaid(@PathVariable String id) {
        return currentUserId()
                .flatMap(uid -> billService.markPaid(id, uid))
                .map(b -> ResponseEntity.ok(ApiResponse.success(b)));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return currentUserId()
                .flatMap(uid -> billService.deleteBill(id, uid))
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    /**
     * Uploads a receipt or photo for an existing bill (multipart/form-data, field name "file").
     * Replaces any previously attached receipt.
     */
    @PostMapping(value = "/{id}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ApiResponse<Bill>>> uploadReceipt(
            @PathVariable String id,
            @RequestPart("file") FilePart file) {
        return currentUserId()
                .flatMap(uid -> billService.uploadReceipt(id, uid, file))
                .map(b -> ResponseEntity.ok(ApiResponse.success(b)));
    }

    /** Removes the receipt attachment from a bill. */
    @DeleteMapping("/{id}/receipt")
    public Mono<ResponseEntity<ApiResponse<Bill>>> deleteReceipt(@PathVariable String id) {
        return currentUserId()
                .flatMap(uid -> billService.deleteReceipt(id, uid))
                .map(b -> ResponseEntity.ok(ApiResponse.success(b)));
    }

    private Mono<String> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ((UserPrincipal) ctx.getAuthentication().getPrincipal()).getUserId());
    }
}
