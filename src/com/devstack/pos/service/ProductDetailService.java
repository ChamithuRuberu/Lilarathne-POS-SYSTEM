package com.devstack.pos.service;

import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.repository.ProductDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductDetailService {
    
    private final ProductDetailRepository productDetailRepository;
    
    public ProductDetail saveProductDetail(ProductDetail productDetail) {
        return productDetailRepository.save(productDetail);
    }
    
    public boolean updateProductDetail(ProductDetail productDetail) {
        if (productDetailRepository.existsById(productDetail.getCode())) {
            productDetailRepository.save(productDetail);
            return true;
        }
        return false;
    }
    
    public boolean deleteProductDetail(String code) {
        if (productDetailRepository.existsById(code)) {
            productDetailRepository.deleteById(code);
            return true;
        }
        return false;
    }
    
    public ProductDetail findProductDetail(String code) {
        return productDetailRepository.findByCode(code).orElse(null);
    }
    
    public List<ProductDetail> findAllProductDetails() {
        return productDetailRepository.findAll();
    }
    
    public List<ProductDetail> findByProductCode(int productCode) {
        return productDetailRepository.findByProductCode(productCode);
    }
    
    public ProductDetail findByCodeWithProduct(String code) {
        return productDetailRepository.findByCodeWithProduct(code).orElse(null);
    }
}

