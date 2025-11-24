package com.devstack.pos.view.tm;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class GeneralItemSelectedTm {
    private String productName;
    private TextField quantity;
    private TextField unitPrice;
    private double total;
    private Button btnRemove;
    
    public GeneralItemSelectedTm() {
    }
    
    public GeneralItemSelectedTm(String productName, TextField quantity, TextField unitPrice, double total, Button btnRemove) {
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = total;
        this.btnRemove = btnRemove;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public TextField getQuantity() {
        return quantity;
    }
    
    public void setQuantity(TextField quantity) {
        this.quantity = quantity;
    }
    
    public TextField getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(TextField unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public double getTotal() {
        return total;
    }
    
    public void setTotal(double total) {
        this.total = total;
    }
    
    public Button getBtnRemove() {
        return btnRemove;
    }
    
    public void setBtnRemove(Button btnRemove) {
        this.btnRemove = btnRemove;
    }
}

