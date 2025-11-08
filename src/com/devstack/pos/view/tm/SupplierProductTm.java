package com.devstack.pos.view.tm;

import javafx.scene.layout.HBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SupplierProductTm {
    private String productName;
    private Double buyingPrice;
    private Integer qtyOnHand;
    private HBox actionButtons;
    
    public SupplierProductTm(String productName, Double buyingPrice, Integer qtyOnHand) {
        this.productName = productName;
        this.buyingPrice = buyingPrice;
        this.qtyOnHand = qtyOnHand;
    }
}

