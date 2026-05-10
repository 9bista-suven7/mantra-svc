package com.rc1.mantra_svc.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Summary of who owes whom in a group (net balances). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSummary {

    private String groupId;
    private String currency;

    /** Maps userId → net balance (positive = owed money, negative = owes money). */
    private Map<String, BigDecimal> netBalances;

    /** Optimised list of "who should pay whom" to settle debts. */
    private List<PaymentSuggestion> suggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSuggestion {
        private String fromUserId;
        private String fromDisplayName;
        private String toUserId;
        private String toDisplayName;
        private BigDecimal amount;
        private String currency;
    }
}
