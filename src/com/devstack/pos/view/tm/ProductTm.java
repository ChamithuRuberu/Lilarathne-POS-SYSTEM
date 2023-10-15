package com.devstack.pos.view.tm;

import javafx.scene.control.Button;

public class ProductTm {
    private int code;
    private String description;
    private Button showMore;
    private Button delete;

    public ProductTm() {
    }

    public ProductTm(int code, String description, Button showMore, Button delete) {
        this.code = code;
        this.description = description;
        this.showMore = showMore;
        this.delete = delete;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
                ", description='" + description + '\'' +
                ", showMore=" + showMore +
                ", delete=" + delete +
                '}';
    }
}
