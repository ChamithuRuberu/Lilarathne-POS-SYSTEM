package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetail {
    @Id
    @Column(name = "code", length = 100)
    private String code;
    
    @Column(name = "barcode", columnDefinition = "TEXT")
    private String barcode;
    
    @Column(name = "qty_on_hand", nullable = false)
    private int qtyOnHand;
    
    @Column(name = "selling_price", nullable = false)
    private double sellingPrice;
    
    @Column(name = "show_price", nullable = false)
    private double showPrice;
    
    @Column(name = "buying_price", nullable = false)
    private double buyingPrice;
    
    @Column(name = "product_code", nullable = false)
    private int productCode;
    
    @Column(name = "discount_availability", nullable = false)
    private boolean discountAvailability;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_code", insertable = false, updatable = false)
    private Product product;
}
