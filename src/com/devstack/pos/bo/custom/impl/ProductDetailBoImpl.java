package com.devstack.pos.bo.custom.impl;

import com.devstack.pos.bo.custom.ProductDetailBo;
import com.devstack.pos.dao.DaoFactory;
import com.devstack.pos.dao.custom.ProductDetailDao;
import com.devstack.pos.dto.ProductDetailDto;
import com.devstack.pos.dto.ProductDetailJoinDto;
import com.devstack.pos.dto.ProductDto;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.enums.DaoType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductDetailBoImpl implements ProductDetailBo {
    ProductDetailDao dao = DaoFactory.getInstance().getDao(DaoType.PRODUCT_DETAIL);

    @Override
    public boolean saveProductDetail(ProductDetailDto dto) throws SQLException, ClassNotFoundException {
        return dao.save(
                new ProductDetail(
                        dto.getCode(), dto.getBarcode(), dto.getQtyOnHand(), dto.getSellingPrice(),
                        dto.getShowPrice(), dto.getBuyingPrice(), dto.getProductCode(),
                        dto.isDiscountAvailability()
                )
        );
    }

    @Override
    public List<ProductDetailDto> findAllProductDetails(int productCode) throws SQLException, ClassNotFoundException {
        List<ProductDetailDto> dtos = new ArrayList<>();
        for (ProductDetail d : dao.findAllProductDetails(productCode)
        ) {
            dtos.add(
                    new ProductDetailDto(
                            d.getCode(), d.getBarcode(), d.getQtyOnHand(), d.getSellingPrice(),
                            d.getShowPrice(), d.getBuyingPrice(), d.getProductCode(),
                            d.isDiscountAvailability()
                    )
            );
        }
        return dtos;
    }

    @Override
    public ProductDetailDto findProductDetail(String code) throws SQLException, ClassNotFoundException {
        ProductDetail d = dao.findProductDetail(code);
        if (d != null) {
            return new ProductDetailDto(
                    d.getCode(), d.getBarcode(), d.getQtyOnHand(), d.getSellingPrice(),
                    d.getShowPrice(), d.getBuyingPrice(), d.getProductCode(),
                    d.isDiscountAvailability()
            );
        }
        return null;
    }

    @Override
    public ProductDetailJoinDto findProductJoinDetail(String code) throws SQLException, ClassNotFoundException {
        return dao.findProductDetailJoinData(code);
    }
}
