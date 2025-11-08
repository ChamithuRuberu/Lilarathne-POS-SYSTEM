package com.devstack.pos.repository;

import com.devstack.pos.entity.ReturnOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * Count items in a return order
     */
    @Query("SELECT COUNT(roi) FROM ReturnOrderItem roi WHERE roi.returnOrderId = :returnOrderId")
    Long countItemsInReturnOrder(@Param("returnOrderId") Integer returnOrderId);
    
    /**
     * Find items where inventory not yet restored
     */
    @Query("SELECT roi FROM ReturnOrderItem roi WHERE roi.inventoryRestored = false")
    List<ReturnOrderItem> findUnrestoredItems();
}

