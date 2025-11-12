package com.devstack.pos.view.tm;

import com.jfoenix.controls.JFXButton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PendingPaymentTm {
    private Long code;
    private String customerName;
    private LocalDateTime issuedDate;
    private String paymentMethod;
    private double totalCost;
    private String operatorEmail;
    private JFXButton completePaymentButton;
    
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    // Formatted properties for table display
    public String getDateFormatted() {
        return issuedDate != null ? issuedDate.format(dateFormatter) : "";
    }
    
    public String getTotalFormatted() {
        return String.format("%.2f /=", totalCost);
    }
    
    // Constructor without button (for backward compatibility)
    public PendingPaymentTm(Long code, String customerName, LocalDateTime issuedDate, 
                           String paymentMethod, double totalCost, String operatorEmail) {
        this.code = code;
        this.customerName = customerName;
        this.issuedDate = issuedDate;
        this.paymentMethod = paymentMethod;
        this.totalCost = totalCost;
        this.operatorEmail = operatorEmail;
    }
}

