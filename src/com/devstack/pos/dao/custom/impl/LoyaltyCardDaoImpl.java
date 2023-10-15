package com.devstack.pos.dao.custom.impl;

import com.devstack.pos.dao.CrudUtil;
import com.devstack.pos.dao.custom.LoyaltyCardDao;
import com.devstack.pos.entity.LoyaltyCard;

import java.sql.SQLException;
import java.util.List;

public class LoyaltyCardDaoImpl implements LoyaltyCardDao {
    @Override
    public boolean save(LoyaltyCard loyaltyCard) throws SQLException, ClassNotFoundException {
        LoyaltyCard l = loyaltyCard;
        return CrudUtil.execute("INSERT INTO loyalty_card VALUES(?,?,?,?)",
                loyaltyCard.getCode(), loyaltyCard.getCardType().name(),
                loyaltyCard.getBarcode(), loyaltyCard.getEmail());
    }

    @Override
    public boolean update(LoyaltyCard loyaltyCard) throws SQLException, ClassNotFoundException {
        return false;
    }

    @Override
    public boolean delete(Integer integer) throws SQLException, ClassNotFoundException {
        return false;
    }

    @Override
    public LoyaltyCard find(Integer integer) throws SQLException, ClassNotFoundException {
        return null;
    }

    @Override
    public List<LoyaltyCard> findAll() throws SQLException, ClassNotFoundException {
        return null;
    }
}
