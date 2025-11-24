package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents individual items/products in a super admin order
 * Links super admin orders to specific products and tracks quantities, prices, and discounts
 */
@Entity
@Table(name = "super_admin_order_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuperAdminOrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "product_code")
    private Integer productCode;
    
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;
    
    @Column(name = "batch_code", length = 100)
    private String batchCode;
    
    @Column(name = "batch_number", length = 50)
    private String batchNumber;
    
    @Column(name = "quantity", nullable = false, columnDefinition = "DECIMAL(10,2)")
    private Double quantity; // Supports decimal quantities (e.g., 2.5 kg, 3.75 meters)
    
    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;
    
    @Column(name = "discount_per_unit")
    private Double discountPerUnit;
    
    @Column(name = "total_discount")
    private Double totalDiscount;
    
    @Column(name = "line_total", nullable = false)
    private Double lineTotal;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // Calculate total discount if not set (supports decimal quantities)
        if (totalDiscount == null && discountPerUnit != null && quantity != null) {
            totalDiscount = discountPerUnit * quantity;
        }
        // Calculate line total if not set (supports decimal quantities)
        if (lineTotal == null && unitPrice != null && quantity != null) {
            double discountAmount = (totalDiscount != null) ? totalDiscount : 0.0;
            lineTotal = (unitPrice * quantity) - discountAmount;
        }
    }
}

