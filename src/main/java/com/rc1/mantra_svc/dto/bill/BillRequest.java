package com.rc1.mantra_svc.dto.bill;

import com.rc1.mantra_svc.model.Bill;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Create / update request for a bill. */
@Data
public class BillRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Category is required")
    private Bill.BillCategory category;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    private String currency = "INR";
    private Instant dueDate;
    private boolean recurring;
    private String recurrencePattern;
    private List<String> tags;
}
