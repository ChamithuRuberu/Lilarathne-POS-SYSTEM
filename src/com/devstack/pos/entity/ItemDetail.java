package com.devstack.pos.entity;

public class ItemDetail implements SuperEntity {
    private String detailCode;
    private int order;
    private int qty;
    private double discount;
    private double amount;

    public ItemDetail() {
    }

    public ItemDetail(String detailCode, int order, int qty, double discount, double amount) {
        this.detailCode = detailCode;
        this.order = order;
        this.qty = qty;
        this.discount = discount;
        this.amount = amount;
    }

    public String getDetailCode() {
        return detailCode;
    }

    public void setDetailCode(String detailCode) {
        this.detailCode = detailCode;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "ItemDetailDto{" +
                "detailCode='" + detailCode + '\'' +
                ", order=" + order +
                ", qty=" + qty +
                ", discount=" + discount +
                ", amount=" + amount +
                '}';
    }
}
