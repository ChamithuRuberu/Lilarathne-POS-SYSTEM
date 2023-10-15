package com.devstack.pos.dto;

import com.devstack.pos.entity.SuperEntity;
import com.devstack.pos.enums.CardType;

public class LoyaltyCardDto implements SuperEntity {
    private int code;
    private CardType cardType;
    private String barcode;
    private String email;

    public LoyaltyCardDto() {
    }

    public LoyaltyCardDto(int code, CardType cardType, String barcode, String email) {
        this.code = code;
        this.cardType = cardType;
        this.barcode = barcode;
        this.email = email;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public CardType getCardType() {
        return cardType;
    }

    public void setCardType(CardType cardType) {
        this.cardType = cardType;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
