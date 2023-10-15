package com.devstack.pos.view.tm;

import javafx.scene.control.Button;

public class CartTm {
    private String code;
    private String description;
    private double discount;
    private double sellingPrice;
    private double showPrice;
    private int qty;
    private double totalCost;
    private Button btn;

    public CartTm() {
    }

    public CartTm(String code, String description, double discount, double sellingPrice, double showPrice, int qty, double totalCost, Button btn) {
        this.code = code;
        this.description = description;
        this.discount = discount;
        this.sellingPrice = sellingPrice;
        this.showPrice = showPrice;
        this.qty = qty;
        this.totalCost = totalCost;
        this.btn = btn;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public double getShowPrice() {
        return showPrice;
    }

    public void setShowPrice(double showPrice) {
        this.showPrice = showPrice;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public Button getBtn() {
        return btn;
    }

    public void setBtn(Button btn) {
        this.btn = btn;
    }
}
