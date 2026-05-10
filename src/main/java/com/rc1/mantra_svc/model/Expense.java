package com.rc1.mantra_svc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * A single expense within an {@link ExpenseGroup}.
 * Tracks who paid and how much each member owes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "expenses")
public class Expense {

    @Id
    private String id;

    @Indexed
    private String groupId;

    private String paidById;
    private String description;
    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";

    private String category;
    private SplitType splitType;

    /** Maps userId → amount that user owes toward this expense. */
    private Map<String, BigDecimal> splits;

    @Builder.Default
    private boolean settled = false;

    private Instant expenseDate;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum SplitType {
        EQUAL, EXACT, PERCENTAGE
    }
}
