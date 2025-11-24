package com.devstack.pos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "super_admin_order_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminOrderDetail {
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
    
    @Column(name = "order_type", length = 20)
    private String orderType = "HARDWARE"; // HARDWARE or CONSTRUCTION
    
    @Column(name = "customer_paid")
    private Double customerPaid;
    
    @Column(name = "balance")
    private Double balance;
}

