package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents individual items being returned in a return order
 * Links return orders to specific products and tracks return quantities and refund amounts
 */
@Entity
@Table(name = "return_order_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnOrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "return_order_id", nullable = false)
    private Integer returnOrderId;
    
    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;
    
    @Column(name = "product_code", nullable = false)
    private Integer productCode;
    
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;
    
    @Column(name = "batch_code", length = 100)
    private String batchCode;
    
    @Column(name = "batch_number", length = 50)
    private String batchNumber;
    
    @Column(name = "original_quantity", nullable = false)
    private Integer originalQuantity;
    
    @Column(name = "return_quantity", nullable = false)
    private Integer returnQuantity;
    
    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;
    
    @Column(name = "refund_amount", nullable = false)
    private Double refundAmount;
    
    @Column(name = "reason", length = 200)
    private String reason;
    
    @Column(name = "inventory_restored", nullable = false)
    private Boolean inventoryRestored = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (inventoryRestored == null) {
            inventoryRestored = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

