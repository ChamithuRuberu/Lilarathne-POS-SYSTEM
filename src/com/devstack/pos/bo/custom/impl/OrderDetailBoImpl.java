package com.devstack.pos.bo.custom.impl;

import com.devstack.pos.bo.custom.OrderDetailBo;
import com.devstack.pos.dao.DaoFactory;
import com.devstack.pos.dao.custom.ItemDetailDao;
import com.devstack.pos.dao.custom.OrderDetailDao;
import com.devstack.pos.dao.custom.ProductDetailDao;
import com.devstack.pos.db.DbConnection;
import com.devstack.pos.dto.ItemDetailDto;
import com.devstack.pos.dto.OrderDetailDto;
import com.devstack.pos.entity.ItemDetail;
import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.enums.DaoType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class OrderDetailBoImpl implements OrderDetailBo {

    OrderDetailDao dao = DaoFactory.getInstance().getDao(DaoType.ORDER_DETAIL);
    ItemDetailDao detailDao = DaoFactory.getInstance().getDao(DaoType.ITEM_DETAIL);
    ProductDetailDao productDetailDao = DaoFactory.getInstance().getDao(DaoType.PRODUCT_DETAIL);

    @Override
    public boolean makeOrder(OrderDetailDto d) throws SQLException {
        Connection connection = null;
        try {
            connection = DbConnection.getInstance().getConnection();
            connection.setAutoCommit(false);
            if (saveOrder(d)) {
                boolean isItemsSaved = saveItemDetails(d.getItemDetailDto(), d.getCode());
                if (isItemsSaved) {
                    connection.commit();
                    return true;
                } else {
                    connection.rollback();
                    return false;
                }
            } else {
                connection.rollback();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.setAutoCommit(true);
        }
        return false;
    }

    private boolean saveOrder(OrderDetailDto dto) throws SQLException, ClassNotFoundException {
        return dao.save(
                new OrderDetail(dto.getCode(),
                        dto.getIssuedDate(), dto.getTotalCost(),
                        dto.getCustomerEmail(), dto.getDiscount(),
                        dto.getOperatorEmail())
        );
    }

    private boolean saveItemDetails(List<ItemDetailDto> list, int orderCode) throws SQLException, ClassNotFoundException {
        for (ItemDetailDto dto : list
        ) {
            boolean isItemSaved = detailDao.save(
                    new ItemDetail(dto.getDetailCode(), orderCode,
                            dto.getQty(), dto.getDiscount(), dto.getAmount())
            );
            if (isItemSaved) {
                if (!updateQty(dto.getDetailCode(), dto.getQty())) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean updateQty(String productCode, int qty) throws SQLException, ClassNotFoundException {
        return productDetailDao.manageQty(productCode, qty);
    }
}
