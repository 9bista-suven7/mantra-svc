package com.rc1.mantra_svc.dto.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** Request to record a payment settlement between two group members. */
@Data
public class SettleRequest {

    @NotBlank(message = "Group ID is required")
    private String groupId;

    @NotBlank(message = "Recipient user ID is required")
    private String toUserId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    private String currency = "INR";
    private String notes;
}
