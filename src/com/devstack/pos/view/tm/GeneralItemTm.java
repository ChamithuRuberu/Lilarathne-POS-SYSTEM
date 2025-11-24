package com.devstack.pos.view.tm;

import javafx.scene.control.CheckBox;

public class GeneralItemTm {
    private String productName;
    private CheckBox select;
    
    public GeneralItemTm() {
    }
    
    public GeneralItemTm(String productName, CheckBox select) {
        this.productName = productName;
        this.select = select;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public CheckBox getSelect() {
        return select;
    }
    
    public void setSelect(CheckBox select) {
        this.select = select;
    }
}

