package com.devstack.pos.repository;

import com.devstack.pos.entity.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductDetailRepository extends JpaRepository<ProductDetail, String> {
    Optional<ProductDetail> findByCode(String code);
    
    List<ProductDetail> findByProductCode(int productCode);
    
    Optional<ProductDetail> findByBarcode(String barcode);
    
    @Query("SELECT pd FROM ProductDetail pd WHERE pd.code = :code OR pd.barcode = :code")
    Optional<ProductDetail> findByCodeOrBarcode(@Param("code") String code);
    
    @Query("SELECT pd FROM ProductDetail pd WHERE pd.supplierName = :supplierName ORDER BY pd.createdAt DESC")
    List<ProductDetail> findBySupplierName(@Param("supplierName") String supplierName);
    
    @Query("SELECT pd FROM ProductDetail pd WHERE pd.supplierName = :supplierName " +
           "AND (:startDate IS NULL OR pd.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR pd.createdAt <= :endDate) " +
           "ORDER BY pd.createdAt DESC")
    List<ProductDetail> findBySupplierNameAndDateRange(
        @Param("supplierName") String supplierName,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}

