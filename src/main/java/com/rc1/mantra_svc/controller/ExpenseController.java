package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.dto.ApiResponse;
import com.rc1.mantra_svc.dto.expense.*;
import com.rc1.mantra_svc.dto.expense.GroupMemberDto;
import com.rc1.mantra_svc.model.Expense;
import com.rc1.mantra_svc.model.ExpenseGroup;
import com.rc1.mantra_svc.model.Settlement;
import com.rc1.mantra_svc.security.UserPrincipal;
import com.rc1.mantra_svc.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Expense splitting REST API.
 */
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    // ── Groups ────────────────────────────────────────────────────────────────

    @PostMapping("/groups")
    public Mono<ResponseEntity<ApiResponse<ExpenseGroup>>> createGroup(
            @Valid @RequestBody CreateGroupRequest request) {
        return currentUserId()
                .flatMap(uid -> expenseService.createGroup(uid, request))
                .map(g -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(g)));
    }

    @GetMapping("/groups")
    public Mono<List<ExpenseGroup>> getMyGroups() {
        return currentUserId()
                .flatMapMany(expenseService::getUserGroups)
                .collectList();
    }

    @GetMapping("/groups/{groupId}")
    public Mono<ResponseEntity<ApiResponse<ExpenseGroup>>> getGroup(@PathVariable String groupId) {
        return currentUserId()
                .flatMap(uid -> expenseService.getGroup(groupId, uid))
                .map(g -> ResponseEntity.ok(ApiResponse.success(g)));
    }

    @DeleteMapping("/groups/{groupId}")
    public Mono<ResponseEntity<Void>> deleteGroup(@PathVariable String groupId) {
        return currentUserId()
                .flatMap(uid -> expenseService.deleteGroup(groupId, uid))
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }

    // ── Expenses ──────────────────────────────────────────────────────────────

    @PostMapping("/groups/{groupId}/expenses")
    public Mono<ResponseEntity<ApiResponse<Expense>>> addExpense(
            @PathVariable String groupId,
            @Valid @RequestBody AddExpenseRequest request) {
        return currentUserId()
                .flatMap(uid -> expenseService.addExpense(groupId, uid, request))
                .map(e -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(e)));
    }

    @GetMapping("/groups/{groupId}/expenses")
    public Mono<List<Expense>> getGroupExpenses(@PathVariable String groupId) {
        return currentUserId()
                .flatMapMany(uid -> expenseService.getGroupExpenses(groupId, uid))
                .collectList();
    }

    @DeleteMapping("/groups/{groupId}/expenses/{expenseId}")
    public Mono<ResponseEntity<Void>> deleteExpense(
            @PathVariable String groupId,
            @PathVariable String expenseId) {
        return currentUserId()
                .flatMap(uid -> expenseService.deleteExpense(groupId, expenseId, uid))
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }

    @GetMapping("/groups/{groupId}/members")
    public Mono<List<GroupMemberDto>> getGroupMembers(@PathVariable String groupId) {
        return currentUserId()
                .flatMapMany(uid -> expenseService.getGroupMembers(groupId, uid))
                .collectList();
    }

    @PostMapping("/groups/{groupId}/members")
    public Mono<ResponseEntity<ApiResponse<ExpenseGroup>>> addMember(
            @PathVariable String groupId,
            @RequestBody java.util.Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim();
        return currentUserId()
                .flatMap(uid -> expenseService.addMember(groupId, uid, email))
                .map(g -> ResponseEntity.ok(ApiResponse.success(g)));
    }

    @DeleteMapping("/groups/{groupId}/members/{userId}")
    public Mono<ResponseEntity<ApiResponse<ExpenseGroup>>> removeMember(
            @PathVariable String groupId,
            @PathVariable String userId) {
        return currentUserId()
                .flatMap(uid -> expenseService.removeMember(groupId, uid, userId))
                .map(g -> ResponseEntity.ok(ApiResponse.success(g)));
    }

    @GetMapping("/groups/{groupId}/balances")
    public Mono<ResponseEntity<ApiResponse<BalanceSummary>>> getBalances(
            @PathVariable String groupId) {
        return currentUserId()
                .flatMap(uid -> expenseService.getGroupBalances(groupId, uid))
                .map(b -> ResponseEntity.ok(ApiResponse.success(b)));
    }

    // ── Settlement ────────────────────────────────────────────────────────────

    @PostMapping("/settle")
    public Mono<ResponseEntity<ApiResponse<Settlement>>> settle(
            @Valid @RequestBody SettleRequest request) {
        return currentUserId()
                .flatMap(uid -> expenseService.settle(uid, request))
                .map(s -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(s)));
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private Mono<String> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ((UserPrincipal) ctx.getAuthentication().getPrincipal()).getUserId());
    }
}
