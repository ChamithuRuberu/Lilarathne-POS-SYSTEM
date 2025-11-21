package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Batch Code / Batch ID

    @Column(name = "code", length = 100)
    private String code; // Batch Code / Batch ID
    
    @Column(name = "batch_number", length = 50)
    private String batchNumber; // Human-readable batch number
    
    @Column(name = "barcode", columnDefinition = "TEXT")
    private String barcode; // Batch barcode image (Base64)
    
    @Column(name = "qty_on_hand", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private double qtyOnHand; // Changed to double to support decimal quantities (e.g., 2.5 kg, 3.75 meters)
    
    @Column(name = "initial_qty", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private double initialQty; // Original quantity when batch was created (supports decimals)
    
    @Column(name = "selling_price", nullable = false)
    private double sellingPrice;
    
    @Column(name = "show_price", nullable = false)
    private double showPrice;
    
    @Column(name = "buying_price", nullable = false)
    private double buyingPrice;
    
    @Column(name = "profit_margin")
    private double profitMargin; // Calculated: (selling - buying) / buying * 100
    
    @Column(name = "product_code", nullable = false)
    private int productCode;
    
    @Column(name = "discount_availability", nullable = false)
    private boolean discountAvailability;
    
    @Column(name = "discount_rate")
    private double discountRate; // Percentage discount if applicable
    
    @Column(name = "supplier_name", length = 200)
    private String supplierName;
    
    @Column(name = "supplier_contact", length = 100)
    private String supplierContact;
    
    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold; // Alert when qty falls below this
    
    @Column(name = "batch_status", length = 50)
    private String batchStatus; // ACTIVE, LOW_STOCK, OUT_OF_STOCK, EXPIRED
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Additional notes about the batch
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    private Long productId;
    
    /**
     * Calculate profit margin percentage
     */
    @PrePersist
    @PreUpdate
    public void calculateProfitMargin() {
        if (buyingPrice > 0) {
            this.profitMargin = ((sellingPrice - buyingPrice) / buyingPrice) * 100;
        }
        
        // For new records, ensure status is set (default to ACTIVE if null)
        if (this.batchStatus == null || this.batchStatus.trim().isEmpty()) {
            this.batchStatus = "ACTIVE";
        }
        
        // Update batch status based on quantity and expiry
        updateBatchStatus();
    }
    
    /**
     * Update batch status based on current state
     * Note: Does not override DELETED status - once deleted, status remains DELETED
     */
    public void updateBatchStatus() {
        // Don't update status if already marked as DELETED
        if ("DELETED".equals(this.batchStatus)) {
            return;
        }
        
        if (expiryDate != null && LocalDate.now().isAfter(expiryDate)) {
            this.batchStatus = "EXPIRED";
        } else if (qtyOnHand <= 0) {
            this.batchStatus = "OUT_OF_STOCK";
        } else if (lowStockThreshold != null && qtyOnHand <= lowStockThreshold) {
            this.batchStatus = "LOW_STOCK";
        } else {
            this.batchStatus = "ACTIVE";
        }
    }
    
    /**
     * Check if batch is expired
     */
    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }
    
    /**
     * Check if batch is low on stock
     */
    public boolean isLowStock() {
        return lowStockThreshold != null && qtyOnHand > 0 && qtyOnHand <= lowStockThreshold;
    }
    
    /**
     * Get days until expiry
     */
    public long getDaysUntilExpiry() {
        if (expiryDate == null) return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }
}
