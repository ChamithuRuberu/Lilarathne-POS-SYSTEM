package com.devstack.pos.view.tm;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Table Model for Return Items
 * Represents products that can be returned from an order
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItemTm {
    private Long orderItemId;
    private Integer productCode;
    private String productName;
    private String batchCode;
    private String batchNumber;
    private Double orderedQuantity; // Original ordered quantity (supports decimal quantities like 2.5, 3.75)
    private Double unitPrice;
    private Double lineTotal;
    private CheckBox selectCheckBox;
    private Spinner<Double> returnQuantitySpinner; // Changed to Double to support decimal quantities
    
    // Constructor without UI components (for data transfer)
    public ReturnItemTm(Long orderItemId, Integer productCode, String productName, 
                       String batchCode, String batchNumber, Double orderedQuantity, 
                       Double unitPrice, Double lineTotal) {
        this.orderItemId = orderItemId;
        this.productCode = productCode;
        this.productName = productName;
        this.batchCode = batchCode;
        this.batchNumber = batchNumber;
        this.orderedQuantity = orderedQuantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }
}

