package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "item_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code")
    private Long code;
    
    @Column(name = "detail_code", length = 100)
    private String detailCode;
    
    @Column(name = "order_code", nullable = false)
    private Integer orderCode;
    
    @Column(name = "qty", nullable = false)
    private int qty;
    
    @Column(name = "discount")
    private double discount;
    
    @Column(name = "amount", nullable = false)
    private double amount;
}
