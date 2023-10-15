package com.devstack.pos.bo.custom.impl;

import com.devstack.pos.bo.custom.LoyaltyCardBo;
import com.devstack.pos.dao.DaoFactory;
import com.devstack.pos.dao.custom.LoyaltyCardDao;
import com.devstack.pos.dto.LoyaltyCardDto;
import com.devstack.pos.entity.LoyaltyCard;
import com.devstack.pos.enums.DaoType;

import java.sql.SQLException;

public class LoyaltyCardBoImpl implements LoyaltyCardBo {

    private LoyaltyCardDao loyaltyCardDao= DaoFactory.getInstance().getDao(DaoType.LOYALTY_CARD);

    @Override
    public boolean saveLoyaltyData(LoyaltyCardDto d) throws SQLException, ClassNotFoundException {
        return loyaltyCardDao.save(
                new LoyaltyCard(
                        d.getCode(),d.getCardType(),d.getBarcode(),d.getEmail()
                )
        );
    }
}
