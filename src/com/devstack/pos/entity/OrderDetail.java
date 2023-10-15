package com.devstack.pos.entity;

import com.devstack.pos.dto.ItemDetailDto;

import java.util.Date;
import java.util.List;

public class OrderDetail implements SuperEntity {
    private int code;
    private Date issuedDate;
    private double totalCost;
    private String customerEmail;
    private double discount;
    private String operatorEmail;

    public OrderDetail() {
    }

    public OrderDetail(int code, Date issuedDate, double totalCost, String customerEmail, double discount, String operatorEmail) {
        this.code = code;
        this.issuedDate = issuedDate;
        this.totalCost = totalCost;
        this.customerEmail = customerEmail;
        this.discount = discount;
        this.operatorEmail = operatorEmail;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Date getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(Date issuedDate) {
        this.issuedDate = issuedDate;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public String getOperatorEmail() {
        return operatorEmail;
    }

    public void setOperatorEmail(String operatorEmail) {
        this.operatorEmail = operatorEmail;
    }


    @Override
    public String toString() {
        return "OrderDetail{" +
                "code=" + code +
                ", issuedDate=" + issuedDate +
                ", totalCost=" + totalCost +
                ", customerEmail='" + customerEmail + '\'' +
                ", discount=" + discount +
                ", operatorEmail='" + operatorEmail + '\'' +
                '}';
    }
}
