package com.rc1.mantra_svc.service;

import com.rc1.mantra_svc.dto.expense.*;
import com.rc1.mantra_svc.dto.expense.GroupMemberDto;
import com.rc1.mantra_svc.exception.ResourceNotFoundException;
import com.rc1.mantra_svc.model.Expense;
import com.rc1.mantra_svc.model.ExpenseGroup;
import com.rc1.mantra_svc.model.Settlement;
import com.rc1.mantra_svc.model.User;
import com.rc1.mantra_svc.repository.ExpenseGroupRepository;
import com.rc1.mantra_svc.repository.ExpenseRepository;
import com.rc1.mantra_svc.repository.SettlementRepository;
import com.rc1.mantra_svc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * Business logic for expense groups, expenses, and balance calculations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseGroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new expense group. Creator is automatically added as a member.
     */
    public Mono<ExpenseGroup> createGroup(String userId, CreateGroupRequest request) {
        return resolveUserIdsByEmails(request.getMemberEmails())
                .collectList()
                .flatMap(memberIds -> {
                    List<String> allMembers = new ArrayList<>(memberIds);
                    if (!allMembers.contains(userId)) {
                        allMembers.add(0, userId);
                    }
                    ExpenseGroup group = ExpenseGroup.builder()
                            .name(request.getName())
                            .description(request.getDescription())
                            .emoji(request.getEmoji() != null ? request.getEmoji() : "💰")
                            .category(request.getCategory() != null ? request.getCategory() : "OTHER")
                            .createdById(userId)
                            .memberIds(allMembers)
                            .build();
                    return groupRepository.save(group);
                });
    }

    /** Lists all active groups for a user (as member or creator). */
    public Flux<ExpenseGroup> getUserGroups(String userId) {
        return groupRepository.findByMemberIdsContainingAndActiveTrue(userId);
    }

    /**
     * Soft-deletes a group by setting active=false.
     * Only the group creator may delete it.
     * Also deletes all associated expenses and settlements.
     */
    public Mono<Void> deleteGroup(String groupId, String userId) {
        return groupRepository.findById(groupId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Group not found")))
                .flatMap(group -> {
                    if (!group.getCreatedById().equals(userId)) {
                        return Mono.error(new ResourceNotFoundException("Only the group creator can delete it"));
                    }
                    group.setActive(false);
                    return groupRepository.save(group)
                            .flatMap(saved -> expenseRepository.deleteByGroupId(groupId)
                                    .then(settlementRepository.deleteByGroupId(groupId)));
                });
    }

    /**
     * Adds a member to a group by email. Only group members may invite others.
     */
    public Mono<ExpenseGroup> addMember(String groupId, String requesterId, String email) {
        return getGroup(groupId, requesterId)
                .flatMap(group -> userRepository.findByEmail(email)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("No user found with email: " + email)))
                        .flatMap(user -> {
                            List<String> ids = group.getMemberIds() != null
                                    ? new ArrayList<>(group.getMemberIds()) : new ArrayList<>();
                            if (ids.contains(user.getId())) {
                                return Mono.just(group); // already a member, idempotent
                            }
                            ids.add(user.getId());
                            group.setMemberIds(ids);
                            return groupRepository.save(group);
                        }));
    }

    /**
     * Removes a member from a group. Requester must be the group creator.
     * Creator cannot remove themselves.
     */
    public Mono<ExpenseGroup> removeMember(String groupId, String requesterId, String targetUserId) {
        return groupRepository.findById(groupId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Group not found")))
                .flatMap(group -> {
                    if (!group.getCreatedById().equals(requesterId)) {
                        return Mono.error(new ResourceNotFoundException("Only the group creator can remove members"));
                    }
                    if (group.getCreatedById().equals(targetUserId)) {
                        return Mono.error(new IllegalArgumentException("Cannot remove the group creator"));
                    }
                    List<String> ids = group.getMemberIds() != null
                            ? new ArrayList<>(group.getMemberIds()) : new ArrayList<>();
                    ids.remove(targetUserId);
                    group.setMemberIds(ids);
                    return groupRepository.save(group);
                });
    }

    /** Gets a single group by ID, ensuring the caller is a member. */
    public Mono<ExpenseGroup> getGroup(String groupId, String userId) {
        return groupRepository.findById(groupId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Group not found")))
                .flatMap(group -> {
                    if (!group.getMemberIds().contains(userId)) {
                        return Mono.error(new ResourceNotFoundException("Group not found"));
                    }
                    return Mono.just(group);
                });
    }

    /**
     * Adds an expense to a group with auto-calculated splits for EQUAL type.
     */
    public Mono<Expense> addExpense(String groupId, String callerId, AddExpenseRequest request) {
        return getGroup(groupId, callerId)
                .flatMap(group -> {
                    String paidById = (request.getPaidByUserId() != null && !request.getPaidByUserId().isBlank())
                            ? request.getPaidByUserId() : callerId;
                    Map<String, BigDecimal> splits = computeSplits(
                            request.getSplitType(), request.getAmount(),
                            group.getMemberIds(), request.getSplits());

                    Expense expense = Expense.builder()
                            .groupId(groupId)
                            .paidById(paidById)
                            .description(request.getDescription())
                            .amount(request.getAmount())
                            .currency(request.getCurrency() != null ? request.getCurrency() : "NPR")
                            .category(request.getCategory())
                            .splitType(request.getSplitType())
                            .splits(splits)
                            .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : Instant.now())
                            .build();
                    return expenseRepository.save(expense);
                });
    }

    /** Deletes an expense from a group (caller must be a group member). */
    public Mono<Void> deleteExpense(String groupId, String expenseId, String userId) {
        return getGroup(groupId, userId)
                .flatMap(group -> expenseRepository.deleteById(expenseId));
    }

    /** Returns all expenses in a group ordered by date descending. */
    public Flux<Expense> getGroupExpenses(String groupId, String userId) {
        return getGroup(groupId, userId)
                .flatMapMany(g -> expenseRepository.findByGroupIdOrderByExpenseDateDesc(groupId));
    }

    /** Returns the list of members in a group with display names and initials. */
    public Flux<GroupMemberDto> getGroupMembers(String groupId, String userId) {
        return getGroup(groupId, userId)
                .flatMapMany(group -> {
                    List<String> ids = group.getMemberIds();
                    if (ids == null || ids.isEmpty()) return Flux.empty();
                    return Flux.fromIterable(ids)
                            .flatMap(id -> userRepository.findById(id)
                                    .map(u -> GroupMemberDto.builder()
                                            .userId(u.getId())
                                            .displayName(u.getDisplayName() != null ? u.getDisplayName() : u.getEmail())
                                            .email(u.getEmail())
                                            .initials(buildInitials(u.getDisplayName() != null ? u.getDisplayName() : u.getEmail()))
                                            .build())
                                    .defaultIfEmpty(GroupMemberDto.builder()
                                            .userId(id)
                                            .displayName(id)
                                            .email("")
                                            .initials("?")
                                            .build()));
                });
    }

    /**
     * Computes net balances for every member in the group.
     * Positive balance = owed money; negative = owes money.
     */
    public Mono<BalanceSummary> getGroupBalances(String groupId, String userId) {
        return getGroup(groupId, userId)
                .flatMap(group -> expenseRepository.findByGroupIdAndSettledFalse(groupId)
                        .collectList()
                        .flatMap(expenses -> {
                            Map<String, BigDecimal> netBalances = new HashMap<>();
                            group.getMemberIds().forEach(id -> netBalances.put(id, BigDecimal.ZERO));

                            // Pairwise raw debts: pairwise[debtor][creditor] = amount debtor owes creditor.
                            Map<String, Map<String, BigDecimal>> pairwise = new HashMap<>();

                            for (Expense e : expenses) {
                                // Payer is owed back by everyone else
                                netBalances.merge(e.getPaidById(), e.getAmount(), BigDecimal::add);
                                // Each person's share is deducted; also accumulate pairwise debt
                                String creditor = e.getPaidById();
                                e.getSplits().forEach((memberId, share) -> {
                                    netBalances.merge(memberId, share.negate(), BigDecimal::add);
                                    if (!memberId.equals(creditor)) {
                                        pairwise
                                                .computeIfAbsent(memberId, k -> new HashMap<>())
                                                .merge(creditor, share, BigDecimal::add);
                                    }
                                });
                            }

                            return Flux.fromIterable(group.getMemberIds())
                                    .flatMap(id -> userRepository.findById(id))
                                    .collectMap(User::getId, User::getDisplayName)
                                    .map(names -> {
                                        List<BalanceSummary.PaymentSuggestion> suggestions =
                                                computePairwiseSettlements(pairwise, names, "USD");
                                        return BalanceSummary.builder()
                                                .groupId(groupId)
                                                .currency("USD")
                                                .netBalances(netBalances)
                                                .suggestions(suggestions)
                                                .build();
                                    });
                        }));
    }

    /** Records a settlement payment and marks related expenses as settled. */
    public Mono<Settlement> settle(String fromUserId, SettleRequest request) {
        return getGroup(request.getGroupId(), fromUserId)
                .flatMap(group -> {
                    Settlement settlement = Settlement.builder()
                            .groupId(request.getGroupId())
                            .fromUserId(fromUserId)
                            .toUserId(request.getToUserId())
                            .amount(request.getAmount())
                            .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                            .notes(request.getNotes())
                            .status(Settlement.SettlementStatus.COMPLETED)
                            .build();
                    return settlementRepository.save(settlement);
                });
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Flux<String> resolveUserIdsByEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) return Flux.empty();
        return Flux.fromIterable(emails)
                .flatMap(email -> userRepository.findByEmail(email)
                        .map(User::getId)
                        .onErrorResume(e -> Mono.empty()));
    }

    private Map<String, BigDecimal> computeSplits(
            Expense.SplitType type, BigDecimal total,
            List<String> memberIds, Map<String, BigDecimal> provided) {

        if (type == Expense.SplitType.EQUAL) {
            BigDecimal share = total.divide(
                    BigDecimal.valueOf(memberIds.size()), 2, RoundingMode.HALF_UP);
            Map<String, BigDecimal> splits = new LinkedHashMap<>();
            memberIds.forEach(id -> splits.put(id, share));
            return splits;
        }
        if (type == Expense.SplitType.PERCENTAGE && provided != null) {
            Map<String, BigDecimal> splits = new LinkedHashMap<>();
            provided.forEach((id, pct) -> splits.put(id,
                    total.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)));
            return splits;
        }
        return provided != null ? provided : Map.of();
    }

    /**
     * Builds raw pairwise settlements: for every unordered pair (A,B) with non-zero net debt,
     * emits a single suggestion in the direction of the net flow. With N members and full mixing,
     * up to N*(N-1)/2 suggestions are produced (e.g. 3 for a 3-person group).
     */
    private List<BalanceSummary.PaymentSuggestion> computePairwiseSettlements(
            Map<String, Map<String, BigDecimal>> pairwise, Map<String, String> names, String currency) {

        List<BalanceSummary.PaymentSuggestion> suggestions = new ArrayList<>();
        Set<String> seenPairs = new HashSet<>();

        for (Map.Entry<String, Map<String, BigDecimal>> debtorEntry : pairwise.entrySet()) {
            String a = debtorEntry.getKey();
            for (Map.Entry<String, BigDecimal> creditorEntry : debtorEntry.getValue().entrySet()) {
                String b = creditorEntry.getKey();
                String pairKey = a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
                if (seenPairs.contains(pairKey)) continue;
                seenPairs.add(pairKey);

                BigDecimal aOwesB = creditorEntry.getValue();
                BigDecimal bOwesA = pairwise.getOrDefault(b, Map.of()).getOrDefault(a, BigDecimal.ZERO);
                BigDecimal net = aOwesB.subtract(bOwesA).setScale(2, RoundingMode.HALF_UP);

                if (net.compareTo(BigDecimal.ZERO) == 0) continue;

                String fromId = net.compareTo(BigDecimal.ZERO) > 0 ? a : b;
                String toId   = net.compareTo(BigDecimal.ZERO) > 0 ? b : a;
                BigDecimal amount = net.abs();

                suggestions.add(BalanceSummary.PaymentSuggestion.builder()
                        .fromUserId(fromId)
                        .fromDisplayName(names.getOrDefault(fromId, "?"))
                        .toUserId(toId)
                        .toDisplayName(names.getOrDefault(toId, "?"))
                        .amount(amount)
                        .currency(currency)
                        .build());
            }
        }
        // Stable order: largest amounts first
        suggestions.sort((x, y) -> y.getAmount().compareTo(x.getAmount()));
        return suggestions;
    }

    private String buildInitials(String displayName) {
        if (displayName == null || displayName.isBlank()) return "?";
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
