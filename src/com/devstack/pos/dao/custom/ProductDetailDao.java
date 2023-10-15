package com.devstack.pos.dao.custom;

import com.devstack.pos.dao.CrudDao;
import com.devstack.pos.dto.ProductDetailDto;
import com.devstack.pos.dto.ProductDetailJoinDto;
import com.devstack.pos.dto.ProductDto;
import com.devstack.pos.entity.ProductDetail;

import java.sql.SQLException;
import java.util.List;

public interface ProductDetailDao extends CrudDao<ProductDetail, String> {
    public List<ProductDetail> findAllProductDetails(int productCode) throws SQLException, ClassNotFoundException;

    public ProductDetail findProductDetail(String code) throws SQLException, ClassNotFoundException;
    public ProductDetailJoinDto findProductDetailJoinData(String code) throws SQLException, ClassNotFoundException;

    public boolean manageQty(String barcode, int qty) throws SQLException, ClassNotFoundException;
}
