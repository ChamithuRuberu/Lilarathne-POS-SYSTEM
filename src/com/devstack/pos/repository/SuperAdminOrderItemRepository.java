package com.devstack.pos.repository;

import com.devstack.pos.entity.SuperAdminOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SuperAdminOrderItemRepository extends JpaRepository<SuperAdminOrderItem, Long> {
    
    /**
     * Find all items for a specific super admin order
     */
    @Query("SELECT o FROM SuperAdminOrderItem o WHERE o.orderId = :orderId ORDER BY o.createdAt ASC")
    List<SuperAdminOrderItem> findByOrderId(@Param("orderId") Long orderId);
    
    // General Items Calculations - Separate methods for general items totals
    
    /**
     * Total revenue from general items only (all time, only PAID orders)
     */
    @Query("SELECT COALESCE(SUM(oi.lineTotal), 0.0) FROM SuperAdminOrderItem oi, SuperAdminOrderDetail od " +
           "WHERE oi.orderId = od.code AND oi.isGeneralItem = true AND od.paymentStatus = 'PAID'")
    Double getGeneralItemsTotalRevenue();
    
    /**
     * Total revenue from general items by date range (only PAID orders)
     */
    @Query("SELECT COALESCE(SUM(oi.lineTotal), 0.0) FROM SuperAdminOrderItem oi, SuperAdminOrderDetail od " +
           "WHERE oi.orderId = od.code AND oi.isGeneralItem = true AND od.paymentStatus = 'PAID' " +
           "AND od.issuedDate BETWEEN :startDate AND :endDate")
    Double getGeneralItemsRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Total quantity of general items sold (all time)
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0.0) FROM SuperAdminOrderItem oi, SuperAdminOrderDetail od " +
           "WHERE oi.orderId = od.code AND oi.isGeneralItem = true AND od.paymentStatus = 'PAID'")
    Double getGeneralItemsTotalQuantity();
    
    /**
     * Total quantity of general items sold by date range
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0.0) FROM SuperAdminOrderItem oi, SuperAdminOrderDetail od " +
           "WHERE oi.orderId = od.code AND oi.isGeneralItem = true AND od.paymentStatus = 'PAID' " +
           "AND od.issuedDate BETWEEN :startDate AND :endDate")
    Double getGeneralItemsQuantityByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count of general items orders (all time)
     */
    @Query("SELECT COUNT(DISTINCT oi.orderId) FROM SuperAdminOrderItem oi, SuperAdminOrderDetail od " +
           "WHERE oi.orderId = od.code AND oi.isGeneralItem = true AND od.paymentStatus = 'PAID'")
    Long getGeneralItemsOrderCount();
    
    /**
     * Count of general items orders by date range
     */
    @Query("SELECT COUNT(DISTINCT oi.orderId) FROM SuperAdminOrderItem oi, SuperAdminOrderDetail od " +
           "WHERE oi.orderId = od.code AND oi.isGeneralItem = true AND od.paymentStatus = 'PAID' " +
           "AND od.issuedDate BETWEEN :startDate AND :endDate")
    Long getGeneralItemsOrderCountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

