package com.devstack.pos.dto;

public class ProductDetailJoinDto {
    private int code ;
    private String description;
    private ProductDetailDto dto;

    public ProductDetailJoinDto() {
    }

    public ProductDetailJoinDto(int code, String description, ProductDetailDto dto) {
        this.code = code;
        this.description = description;
        this.dto = dto;
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

    public ProductDetailDto getDto() {
        return dto;
    }

    public void setDto(ProductDetailDto dto) {
        this.dto = dto;
    }

    @Override
    public String toString() {
        return "ProductDetailJoinDto{" +
                "code=" + code +
                ", description='" + description + '\'' +
                ", dto=" + dto +
                '}';
    }
}
