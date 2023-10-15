package com.devstack.pos.view.tm;

import javafx.scene.control.Button;

public class ProductDetailTm {
    private String code;
    private int qty;
    private double sellingPrice;
    private double buyingPrice;
    private boolean discountAvailability;
    private double showPrice;
    private Button delete;

    public ProductDetailTm() {
    }

    public ProductDetailTm(String code, int qty, double sellingPrice, double buyingPrice, boolean discountAvailability, double showPrice, Button delete) {
        this.code = code;
        this.qty = qty;
        this.sellingPrice = sellingPrice;
        this.buyingPrice = buyingPrice;
        this.discountAvailability = discountAvailability;
        this.showPrice = showPrice;
        this.delete = delete;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public double getBuyingPrice() {
        return buyingPrice;
    }

    public void setBuyingPrice(double buyingPrice) {
        this.buyingPrice = buyingPrice;
    }

    public boolean isDiscountAvailability() {
        return discountAvailability;
    }

    public void setDiscountAvailability(boolean discountAvailability) {
        this.discountAvailability = discountAvailability;
    }

    public double getShowPrice() {
        return showPrice;
    }

    public void setShowPrice(double showPrice) {
        this.showPrice = showPrice;
    }

    public Button getDelete() {
        return delete;
    }

    public void setDelete(Button delete) {
        this.delete = delete;
    }
}
