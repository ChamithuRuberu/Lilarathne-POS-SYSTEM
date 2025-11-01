package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code")
    private Integer code;
    
    @Column(name = "issued_date", nullable = false)
    private LocalDateTime issuedDate;
    
    @Column(name = "total_cost", nullable = false)
    private double totalCost;
    
    @Column(name = "customer_email", length = 100)
    private String customerEmail;
    
    @Column(name = "discount")
    private double discount;
    
    @Column(name = "operator_email", length = 100)
    private String operatorEmail;
}
