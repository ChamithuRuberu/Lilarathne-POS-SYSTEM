package com.devstack.pos.view.tm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderTm {
    private Long code;
    private String customerEmail;
    private LocalDateTime issuedDate;
    private double discount;
    private String operatorEmail;
    private double totalCost;
}

