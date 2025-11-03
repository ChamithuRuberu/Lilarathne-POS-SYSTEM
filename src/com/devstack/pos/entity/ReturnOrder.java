package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "return_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "return_id", unique = true, nullable = false)
    private String returnId;
    
    @Column(name = "order_id", nullable = false)
    private Integer orderId;
    
    @Column(name = "customer_email", nullable = false)
    private String customerEmail;
    
    @Column(name = "original_amount", nullable = false)
    private Double originalAmount;
    
    @Column(name = "refund_amount", nullable = false)
    private Double refundAmount;
    
    @Column(name = "return_reason", nullable = false)
    private String returnReason;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "status", nullable = false)
    private String status; // PENDING, APPROVED, REJECTED, COMPLETED
    
    @Column(name = "processed_by")
    private String processedBy;
    
    @Column(name = "return_date", nullable = false)
    private LocalDateTime returnDate;
    
    @Column(name = "approval_date")
    private LocalDateTime approvalDate;
    
    @Column(name = "completion_date")
    private LocalDateTime completionDate;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        returnDate = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (returnId == null) {
            returnId = generateReturnId();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    private String generateReturnId() {
        return "RET-" + System.currentTimeMillis();
    }
}

