package com.devstack.pos.repository;

import com.devstack.pos.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    /**
     * Find all items for a specific order
     */
    List<OrderItem> findByOrderId(Long orderId);
    
    /**
     * Find all items for a specific product
     */
    List<OrderItem> findByProductCode(Integer productCode);
    
    /**
     * Find all items from a specific batch
     */
    List<OrderItem> findByBatchCode(String batchCode);
    
    /**
     * Get total quantity sold for a product
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.productCode = :productCode")
    Integer getTotalQuantitySoldByProduct(@Param("productCode") Integer productCode);
    
    /**
     * Get total revenue from a product
     */
    @Query("SELECT COALESCE(SUM(oi.lineTotal), 0.0) FROM OrderItem oi WHERE oi.productCode = :productCode")
    Double getTotalRevenueByProduct(@Param("productCode") Integer productCode);
    
    /**
     * Count items in an order
     */
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.orderId = :orderId")
    Long countItemsInOrder(@Param("orderId") Long orderId);
}

