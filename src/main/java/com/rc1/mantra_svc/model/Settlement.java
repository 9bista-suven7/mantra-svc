package com.rc1.mantra_svc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Records a payment settlement between two members of an {@link ExpenseGroup}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "settlements")
public class Settlement {

    @Id
    private String id;

    @Indexed
    private String groupId;

    private String fromUserId;
    private String toUserId;
    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";

    private String notes;

    @Builder.Default
    private SettlementStatus status = SettlementStatus.COMPLETED;

    @CreatedDate
    private Instant settledAt;

    public enum SettlementStatus {
        PENDING, COMPLETED, CANCELLED
    }
}
