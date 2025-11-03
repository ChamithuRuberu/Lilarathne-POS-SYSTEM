package com.devstack.pos.repository;

import com.devstack.pos.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {
    
    Optional<PurchaseOrder> findByPoNumber(String poNumber);
    
    List<PurchaseOrder> findBySupplierName(String supplierName);
    
    List<PurchaseOrder> findByStatus(String status);
    
    @Query("SELECT p FROM PurchaseOrder p WHERE p.orderDate BETWEEN :startDate AND :endDate")
    List<PurchaseOrder> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM PurchaseOrder p WHERE p.status = :status AND p.orderDate BETWEEN :startDate AND :endDate")
    List<PurchaseOrder> findByStatusAndOrderDateBetween(@Param("status") String status,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM PurchaseOrder p WHERE " +
           "(:poNumber IS NULL OR p.poNumber LIKE %:poNumber%) AND " +
           "(:supplierName IS NULL OR p.supplierName LIKE %:supplierName%) AND " +
           "(:status IS NULL OR :status = 'All' OR p.status = :status) AND " +
           "p.orderDate BETWEEN :startDate AND :endDate " +
           "ORDER BY p.orderDate DESC")
    List<PurchaseOrder> searchPurchaseOrders(@Param("poNumber") String poNumber,
                                              @Param("supplierName") String supplierName,
                                              @Param("status") String status,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(p) FROM PurchaseOrder p WHERE p.status = :status")
    Long countByStatus(@Param("status") String status);
    
    @Query("SELECT SUM(p.totalAmount) FROM PurchaseOrder p WHERE p.status = 'RECEIVED'")
    Double getTotalPurchaseAmount();
    
    @Query("SELECT SUM(p.totalAmount) FROM PurchaseOrder p WHERE p.status = 'RECEIVED' AND p.orderDate BETWEEN :startDate AND :endDate")
    Double getTotalPurchaseAmountByDateRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(p.totalAmount - p.paidAmount) FROM PurchaseOrder p WHERE p.status IN ('PENDING', 'APPROVED')")
    Double getTotalOutstandingAmount();
    
    @Query("SELECT COUNT(p) FROM PurchaseOrder p WHERE p.orderDate BETWEEN :startDate AND :endDate")
    Long countPurchasesByDateRange(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);
}

