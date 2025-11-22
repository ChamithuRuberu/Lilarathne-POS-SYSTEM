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
    private Long code;
    
    @Column(name = "issued_date", nullable = false)
    private LocalDateTime issuedDate;
    
    @Column(name = "total_cost", nullable = false)
    private double totalCost;
    
    @Column(name = "customer_id")
    private Long customerId;
    
    @Column(name = "customer_name", length = 100)
    private String customerName;
    
    @Column(name = "discount")
    private double discount;
    
    @Column(name = "operator_email", length = 100)
    private String operatorEmail;
    
    @Column(name = "payment_method", length = 20)
    private String paymentMethod = "CASH";
    
    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "PAID";
    
    @Column(name = "customer_paid")
    private Double customerPaid;
    
    @Column(name = "balance")
    private Double balance;
    
    // Note: We use customerId instead of @ManyToOne relationship to avoid lazy loading issues
    // and to support guest orders (where customerId can be null)
}
