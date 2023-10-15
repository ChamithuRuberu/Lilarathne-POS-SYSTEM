package com.devstack.pos.bo.custom;

import com.devstack.pos.dto.LoyaltyCardDto;

import java.sql.SQLException;

public interface LoyaltyCardBo {
    public boolean saveLoyaltyData(LoyaltyCardDto d) throws SQLException, ClassNotFoundException;
}
