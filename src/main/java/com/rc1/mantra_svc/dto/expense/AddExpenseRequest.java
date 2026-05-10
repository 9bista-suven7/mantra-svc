package com.rc1.mantra_svc.dto.expense;

import com.rc1.mantra_svc.model.Expense;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Request to add an expense to a group. */
@Data
public class AddExpenseRequest {

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    private String currency = "INR";
    private String category;

    @NotNull(message = "Split type is required")
    private Expense.SplitType splitType;

    /**
     * For EXACT splits: maps userId → amount owed.
     * For PERCENTAGE splits: maps userId → percentage (0–100).
     * For EQUAL splits: leave null (computed by the service).
     */
    private Map<String, BigDecimal> splits;

    private Instant expenseDate;

    /**
     * Optional: the user who paid for the expense.
     * Defaults to the authenticated caller when not provided.
     */
    private String paidByUserId;
}
