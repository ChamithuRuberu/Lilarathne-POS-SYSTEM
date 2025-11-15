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
public class OrderTm {
    private Long code;
    private String customerName;
    private LocalDateTime issuedDate;
    private double discount;
    private String operatorEmail;
    private double totalCost;
    private String orderType; // HARDWARE or CONSTRUCTION
    private String productNames; // Comma-separated list of product names
    private JFXButton viewButton;
    private JFXButton returnOrdersButton;
    
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    // Formatted properties for table display
    public String getDateFormatted() {
        return issuedDate != null ? issuedDate.format(dateFormatter) : "";
    }
    
    public String getDiscountFormatted() {
        return String.format("%.2f", discount);
    }
    
    public String getTotalFormatted() {
        return String.format("%.2f /=", totalCost);
    }
    
    // Constructor without button (for backward compatibility)
    public OrderTm(Long code, String customerName, LocalDateTime issuedDate, 
                   double discount, String operatorEmail, double totalCost) {
        this.code = code;
        this.customerName = customerName;
        this.issuedDate = issuedDate;
        this.discount = discount;
        this.operatorEmail = operatorEmail;
        this.totalCost = totalCost;
    }
    
    // Getter for formatted order type display
    public String getOrderTypeFormatted() {
        if (orderType == null) {
            return "Hardware";
        }
        return orderType.equals("CONSTRUCTION") ? "Construction" : "Hardware";
    }
    
    // Getter for return orders button text (for display)
    public String getReturnOrdersText() {
        return returnOrdersButton != null ? "View Returns" : "No Returns";
    }
    
    // Getter for product names (for display)
    public String getProductNames() {
        return productNames != null ? productNames : "";
    }
}
