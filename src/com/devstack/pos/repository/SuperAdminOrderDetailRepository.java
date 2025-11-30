package com.devstack.pos.repository;

import com.devstack.pos.entity.SuperAdminOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SuperAdminOrderDetailRepository extends JpaRepository<SuperAdminOrderDetail, Long> {
    
    // Total revenue from super admin orders (only PAID orders)
    @Query("SELECT SUM(o.totalCost) FROM SuperAdminOrderDetail o WHERE o.paymentStatus = 'PAID'")
    Double getSuperAdminTotalRevenue();
    
    // Total revenue by date range (only PAID orders)
    @Query("SELECT SUM(o.totalCost) FROM SuperAdminOrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate AND o.paymentStatus = 'PAID'")
    Double getSuperAdminRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Total count of super admin orders
    @Query("SELECT COUNT(o) FROM SuperAdminOrderDetail o")
    Long getSuperAdminTotalOrderCount();
    
    // Count orders by date range
    @Query("SELECT COUNT(o) FROM SuperAdminOrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
    Long countSuperAdminOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Average order value (only PAID orders)
    @Query("SELECT AVG(o.totalCost) FROM SuperAdminOrderDetail o WHERE o.paymentStatus = 'PAID'")
    Double getSuperAdminAverageOrderValue();
    
    // Average order value by date range (only PAID orders)
    @Query("SELECT AVG(o.totalCost) FROM SuperAdminOrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate AND o.paymentStatus = 'PAID'")
    Double getSuperAdminAverageOrderValueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find orders by date range
    @Query("SELECT o FROM SuperAdminOrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
    List<SuperAdminOrderDetail> findSuperAdminOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find all pending payments
    @Query("SELECT o FROM SuperAdminOrderDetail o WHERE o.paymentStatus = 'PENDING' ORDER BY o.issuedDate ASC")
    List<SuperAdminOrderDetail> findPendingPayments();
    
    // Find pending payments by payment method
    @Query("SELECT o FROM SuperAdminOrderDetail o WHERE o.paymentStatus = 'PENDING' AND o.paymentMethod = :paymentMethod ORDER BY o.issuedDate ASC")
    List<SuperAdminOrderDetail> findPendingPaymentsByMethod(@Param("paymentMethod") String paymentMethod);
}

