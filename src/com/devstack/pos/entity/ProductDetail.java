package com.devstack.pos.entity;

public class ProductDetail implements SuperEntity {
    private String code;
    private String barcode;
    private int qtyOnHand;
    private double sellingPrice;
    private double showPrice;
    private double buyingPrice;
    private int productCode;
    private boolean discountAvailability;

    public ProductDetail() {
    }

    public ProductDetail(String code, String barcode, int qtyOnHand, double sellingPrice, double showPrice, double buyingPrice, int productCode, boolean discountAvailability) {
        this.code = code;
        this.barcode = barcode;
        this.qtyOnHand = qtyOnHand;
        this.sellingPrice = sellingPrice;
        this.showPrice = showPrice;
        this.buyingPrice = buyingPrice;
        this.productCode = productCode;
        this.discountAvailability = discountAvailability;
    }

    public boolean isDiscountAvailability() {
        return discountAvailability;
    }

    public void setDiscountAvailability(boolean discountAvailability) {
        this.discountAvailability = discountAvailability;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public int getQtyOnHand() {
        return qtyOnHand;
    }

    public void setQtyOnHand(int qtyOnHand) {
        this.qtyOnHand = qtyOnHand;
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

    public double getBuyingPrice() {
        return buyingPrice;
    }

    public void setBuyingPrice(double buyingPrice) {
        this.buyingPrice = buyingPrice;
    }

    public int getProductCode() {
        return productCode;
    }

    public void setProductCode(int productCode) {
        this.productCode = productCode;
    }

    @Override
    public String toString() {
        return "Batch{" +
                "code=" + code +
                ", barcode='" + barcode + '\'' +
                ", qtyOnHand=" + qtyOnHand +
                ", sellingPrice=" + sellingPrice +
                ", showPrice=" + showPrice +
                ", buyingPrice=" + buyingPrice +
                ", productCode=" + productCode +
                '}';
    }
}
