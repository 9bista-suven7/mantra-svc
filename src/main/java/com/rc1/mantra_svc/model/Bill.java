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
import java.util.List;

/**
 * A bill or document (utility, subscription, rent, etc.) with due-date tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bills")
public class Bill {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String title;
    private String description;
    private BillCategory category;
    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";

    private Instant dueDate;

    @Builder.Default
    private BillStatus status = BillStatus.UNPAID;

    @Builder.Default
    private boolean recurring = false;

    /** Cron-like pattern or human-readable: "MONTHLY", "WEEKLY". */
    private String recurrencePattern;

    private String fileUrl;
    private String fileName;
    private String fileType;
    private List<String> tags;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum BillCategory {
        UTILITY, SUBSCRIPTION, RENT, INSURANCE, MEDICAL,
        EDUCATION, ENTERTAINMENT, FOOD, TRANSPORT, OTHER
    }

    public enum BillStatus {
        UNPAID, PAID, OVERDUE, CANCELLED
    }
}
