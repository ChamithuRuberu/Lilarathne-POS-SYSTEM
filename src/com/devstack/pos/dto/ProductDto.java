package com.devstack.pos.dto;

public class ProductDto {
    private int code ;
    private String description;

    public ProductDto() {
    }

    public ProductDto(int code, String description) {
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
        return "ProductDto{" +
                "code=" + code +
                ", description='" + description + '\'' +
                '}';
    }
}
