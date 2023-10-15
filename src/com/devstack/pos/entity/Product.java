package com.devstack.pos.entity;

public class Product implements SuperEntity{
    private int code ;
    private String description;

    public Product() {
    }

    public Product(int code, String description) {
        this.code = code;
        this.description = description;
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

    @Override
    public String toString() {
        return "Product{" +
                "code=" + code +
                ", description='" + description + '\'' +
                '}';
    }
}
