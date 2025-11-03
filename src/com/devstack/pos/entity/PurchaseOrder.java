package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "po_number", unique = true, nullable = false)
    private String poNumber;
    
    @Column(name = "supplier_name", nullable = false)
    private String supplierName;
    
    @Column(name = "supplier_email")
    private String supplierEmail;
    
    @Column(name = "supplier_phone")
    private String supplierPhone;
    
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;
    
    @Column(name = "paid_amount")
    private Double paidAmount;
    
    @Column(name = "status", nullable = false)
    private String status; // PENDING, APPROVED, RECEIVED, CANCELLED
    
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;
    
    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;
    
    @Column(name = "received_date")
    private LocalDateTime receivedDate;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "approved_by")
    private String approvedBy;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        orderDate = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (poNumber == null) {
            poNumber = generatePONumber();
        }
        if (paidAmount == null) {
            paidAmount = 0.0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    private String generatePONumber() {
        return "PO-" + System.currentTimeMillis();
    }
}

