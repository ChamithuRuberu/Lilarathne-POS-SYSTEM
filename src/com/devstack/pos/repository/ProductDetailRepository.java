package com.devstack.pos.repository;

import com.devstack.pos.entity.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductDetailRepository extends JpaRepository<ProductDetail, String> {
    Optional<ProductDetail> findByCode(String code);
    
    List<ProductDetail> findByProductCode(int productCode);
    

    Optional<ProductDetail> findByProductCode(Long code);
}

