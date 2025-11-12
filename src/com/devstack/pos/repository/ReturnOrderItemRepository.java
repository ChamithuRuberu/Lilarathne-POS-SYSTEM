package com.devstack.pos.repository;

import com.devstack.pos.entity.ReturnOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReturnOrderItemRepository extends JpaRepository<ReturnOrderItem, Long> {
    
    /**
     * Find all items for a specific return order
     */
    List<ReturnOrderItem> findByReturnOrderId(Integer returnOrderId);
    
    /**
     * Find all returns for a specific product
     */
    List<ReturnOrderItem> findByProductCode(Integer productCode);
    
    /**
     * Find all returns from a specific batch
     */
    List<ReturnOrderItem> findByBatchCode(String batchCode);
    
    /**
     * Find returns for a specific order item
     */
    List<ReturnOrderItem> findByOrderItemId(Long orderItemId);
    
    /**
     * Get total quantity returned for a product
     */
    @Query("SELECT COALESCE(SUM(roi.returnQuantity), 0) FROM ReturnOrderItem roi WHERE roi.productCode = :productCode")
    Integer getTotalQuantityReturnedByProduct(@Param("productCode") Integer productCode);
    
    /**
     * Get total refund amount for a product
     */
    @Query("SELECT COALESCE(SUM(roi.refundAmount), 0.0) FROM ReturnOrderItem roi WHERE roi.productCode = :productCode")
    Double getTotalRefundByProduct(@Param("productCode") Integer productCode);
    
    /**
     * Get total refund amount for a product within date range
     */
    @Query("SELECT COALESCE(SUM(roi.refundAmount), 0.0) FROM ReturnOrderItem roi " +
           "JOIN ReturnOrder ro ON roi.returnOrderId = ro.id " +
           "WHERE roi.productCode = :productCode AND ro.returnDate BETWEEN :startDate AND :endDate")
    Double getTotalRefundByProductAndDateRange(@Param("productCode") Integer productCode,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get refund amounts and quantities grouped by product code (with date range)
     * Returns: productCode, totalRefundAmount, totalReturnedQuantity
     */
    @Query("SELECT roi.productCode, COALESCE(SUM(roi.refundAmount), 0.0) as totalRefund, COALESCE(SUM(roi.returnQuantity), 0) as totalReturnedQty " +
           "FROM ReturnOrderItem roi " +
           "JOIN ReturnOrder ro ON roi.returnOrderId = ro.id " +
           "WHERE ro.returnDate BETWEEN :startDate AND :endDate " +
           "GROUP BY roi.productCode")
    List<Object[]> getRefundsAndQuantitiesByProductByDateRange(@Param("startDate") LocalDateTime startDate,
                                                                 @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get refund amounts and quantities grouped by product code (all time)
     * Returns: productCode, totalRefundAmount, totalReturnedQuantity
     */
    @Query("SELECT roi.productCode, COALESCE(SUM(roi.refundAmount), 0.0) as totalRefund, COALESCE(SUM(roi.returnQuantity), 0) as totalReturnedQty " +
           "FROM ReturnOrderItem roi " +
           "GROUP BY roi.productCode")
    List<Object[]> getRefundsAndQuantitiesByProduct();
    
    /**
     * Get refund amounts grouped by product code (with date range)
     * Returns: productCode, totalRefundAmount
     */
    @Query("SELECT roi.productCode, COALESCE(SUM(roi.refundAmount), 0.0) as totalRefund " +
           "FROM ReturnOrderItem roi " +
           "JOIN ReturnOrder ro ON roi.returnOrderId = ro.id " +
           "WHERE ro.returnDate BETWEEN :startDate AND :endDate " +
           "GROUP BY roi.productCode")
    List<Object[]> getRefundsByProductByDateRange(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get refund amounts grouped by product code (all time)
     * Returns: productCode, totalRefundAmount
     */
    @Query("SELECT roi.productCode, COALESCE(SUM(roi.refundAmount), 0.0) as totalRefund " +
           "FROM ReturnOrderItem roi " +
           "GROUP BY roi.productCode")
    List<Object[]> getRefundsByProduct();
    
    /**
     * Count items in a return order
     */
    @Query("SELECT COUNT(roi) FROM ReturnOrderItem roi WHERE roi.returnOrderId = :returnOrderId")
    Long countItemsInReturnOrder(@Param("returnOrderId") Integer returnOrderId);
    
    /**
     * Find items where inventory not yet restored
     */
    @Query("SELECT roi FROM ReturnOrderItem roi WHERE roi.inventoryRestored = false")
    List<ReturnOrderItem> findUnrestoredItems();
    
    /**
     * Get refund amounts grouped by category (with date range)
     * Returns: categoryName, totalRefundAmount
     */
    @Query("SELECT COALESCE(c.name, 'Uncategorized'), COALESCE(SUM(roi.refundAmount), 0.0) as totalRefund " +
           "FROM ReturnOrderItem roi " +
           "JOIN ReturnOrder ro ON roi.returnOrderId = ro.id " +
           "JOIN Product p ON roi.productCode = p.code " +
           "LEFT JOIN Category c ON p.category = c " +
           "WHERE ro.returnDate BETWEEN :startDate AND :endDate " +
           "GROUP BY c.name")
    List<Object[]> getRefundsByCategoryByDateRange(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get refund amounts grouped by category (all time)
     * Returns: categoryName, totalRefundAmount
     */
    @Query("SELECT COALESCE(c.name, 'Uncategorized'), COALESCE(SUM(roi.refundAmount), 0.0) as totalRefund " +
           "FROM ReturnOrderItem roi " +
           "JOIN Product p ON roi.productCode = p.code " +
           "LEFT JOIN Category c ON p.category = c " +
           "GROUP BY c.name")
    List<Object[]> getRefundsByCategory();
    
    /**
     * Get refund amounts grouped by customer (with date range)
     * Returns: customerEmail, totalRefundAmount
     */
    @Query("SELECT ro.customerEmail, COALESCE(SUM(roi.refundAmount), 0.0) as totalRefund " +
           "FROM ReturnOrderItem roi " +
           "JOIN ReturnOrder ro ON roi.returnOrderId = ro.id " +
           "WHERE ro.returnDate BETWEEN :startDate AND :endDate " +
           "GROUP BY ro.customerEmail")
    List<Object[]> getRefundsByCustomerByDateRange(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get refund amounts grouped by customer (all time)
     * Returns: customerEmail, totalRefundAmount
     */
    @Query("SELECT ro.customerEmail, COALESCE(SUM(roi.refundAmount), 0.0) as totalRefund " +
           "FROM ReturnOrderItem roi " +
           "JOIN ReturnOrder ro ON roi.returnOrderId = ro.id " +
           "GROUP BY ro.customerEmail")
    List<Object[]> getRefundsByCustomer();
}

