package com.devstack.pos.view.tm;

import javafx.scene.control.Button;

public class ProductTm {
    private int code;
    private String barcode;
    private String description;
    private String category;
    private Button viewBarcode;
    private Button showMore;
    private Button delete;

    public ProductTm() {
    }

    public ProductTm(int code, String barcode, String description, String category, Button viewBarcode, Button showMore, Button delete) {
        this.code = code;
        this.barcode = barcode;
        this.description = description;
        this.category = category;
        this.viewBarcode = viewBarcode;
        this.showMore = showMore;
        this.delete = delete;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Button getViewBarcode() {
        return viewBarcode;
    }

    public void setViewBarcode(Button viewBarcode) {
        this.viewBarcode = viewBarcode;
    }

    public Button getShowMore() {
        return showMore;
    }

    public void setShowMore(Button showMore) {
        this.showMore = showMore;
    }

    public Button getDelete() {
        return delete;
    }

    public void setDelete(Button delete) {
        this.delete = delete;
    }

    @Override
    public String toString() {
        return "ProductTm{" +
                "code=" + code +
                ", barcode='" + barcode + '\'' +
                ", description='" + description + '\'' +
                ", category='" + category + '\'' +
                ", viewBarcode=" + viewBarcode +
                ", showMore=" + showMore +
                ", delete=" + delete +
                '}';
    }
}
