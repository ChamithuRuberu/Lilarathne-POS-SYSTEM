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
    
    @Query("SELECT pd FROM ProductDetail pd WHERE pd.productCode = :productCode")
    List<ProductDetail> findDetailsByProductCode(@Param("productCode") int productCode);
    
    @Query("SELECT pd FROM ProductDetail pd JOIN FETCH pd.product WHERE pd.code = :code")
    Optional<ProductDetail> findByCodeWithProduct(@Param("code") String code);
}

