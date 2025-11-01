package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
