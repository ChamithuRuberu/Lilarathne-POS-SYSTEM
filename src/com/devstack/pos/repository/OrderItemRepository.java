package com.devstack.pos.repository;

import com.devstack.pos.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
     * Get total quantity sold for a product (all orders, including pending payments)
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.productCode = :productCode")
    Integer getTotalQuantitySoldByProduct(@Param("productCode") Integer productCode);
    
    /**
     * Get total revenue from a product (only PAID orders)
     */
    @Query("SELECT COALESCE(SUM(oi.lineTotal), 0.0) FROM OrderItem oi JOIN OrderDetail od ON oi.orderId = od.code WHERE oi.productCode = :productCode AND od.paymentStatus = 'PAID'")
    Double getTotalRevenueByProduct(@Param("productCode") Integer productCode);
    
    /**
     * Count items in an order
     */
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.orderId = :orderId")
    Long countItemsInOrder(@Param("orderId") Long orderId);
    
    /**
     * Get top selling products by quantity (with date range, all orders including pending payments)
     * Returns: productCode, productName, totalQuantity
     */
    @Query("SELECT oi.productCode, oi.productName, SUM(oi.quantity) as totalQuantity " +
           "FROM OrderItem oi " +
           "JOIN OrderDetail od ON oi.orderId = od.code " +
           "WHERE od.issuedDate BETWEEN :startDate AND :endDate " +
           "GROUP BY oi.productCode, oi.productName " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> getTopSellingProductsByQuantity(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get top selling products by quantity (all time, all orders including pending payments)
     * Returns: productCode, productName, totalQuantity
     */
    @Query("SELECT oi.productCode, oi.productName, SUM(oi.quantity) as totalQuantity " +
           "FROM OrderItem oi " +
           "GROUP BY oi.productCode, oi.productName " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> getTopSellingProductsByQuantity();
    
    /**
     * Get top selling products with revenue (with date range, quantity from all orders, revenue only PAID)
     * Returns: productCode, productName, totalQuantity, totalRevenue
     */
    @Query("SELECT oi.productCode, oi.productName, SUM(oi.quantity) as totalQuantity, SUM(CASE WHEN od.paymentStatus = 'PAID' THEN oi.lineTotal ELSE 0 END) as totalRevenue " +
           "FROM OrderItem oi " +
           "JOIN OrderDetail od ON oi.orderId = od.code " +
           "WHERE od.issuedDate BETWEEN :startDate AND :endDate " +
           "GROUP BY oi.productCode, oi.productName " +
           "ORDER BY totalRevenue DESC")
    List<Object[]> getTopSellingProductsWithRevenue(@Param("startDate") LocalDateTime startDate, 
                                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get top selling products with revenue (all time, quantity from all orders, revenue only PAID)
     * Returns: productCode, productName, totalQuantity, totalRevenue
     */
    @Query("SELECT oi.productCode, oi.productName, SUM(oi.quantity) as totalQuantity, SUM(CASE WHEN od.paymentStatus = 'PAID' THEN oi.lineTotal ELSE 0 END) as totalRevenue " +
           "FROM OrderItem oi " +
           "JOIN OrderDetail od ON oi.orderId = od.code " +
           "GROUP BY oi.productCode, oi.productName " +
           "ORDER BY totalRevenue DESC")
    List<Object[]> getTopSellingProductsWithRevenue();
    
    /**
     * Get sales by category (with date range, orderCount from all orders, revenue only PAID)
     * Returns: categoryName, orderCount, totalRevenue
     */
    @Query("SELECT COALESCE(c.name, 'Uncategorized'), COUNT(DISTINCT od.code) as orderCount, SUM(CASE WHEN od.paymentStatus = 'PAID' THEN oi.lineTotal ELSE 0 END) as totalRevenue " +
           "FROM OrderItem oi " +
           "JOIN OrderDetail od ON oi.orderId = od.code " +
           "JOIN Product p ON oi.productCode = p.code " +
           "LEFT JOIN Category c ON p.category = c " +
           "WHERE od.issuedDate BETWEEN :startDate AND :endDate " +
           "GROUP BY c.name " +
           "ORDER BY totalRevenue DESC")
    List<Object[]> getSalesByCategory(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get sales by category (all time, orderCount from all orders, revenue only PAID)
     * Returns: categoryName, orderCount, totalRevenue
     */
    @Query("SELECT COALESCE(c.name, 'Uncategorized'), COUNT(DISTINCT od.code) as orderCount, SUM(CASE WHEN od.paymentStatus = 'PAID' THEN oi.lineTotal ELSE 0 END) as totalRevenue " +
           "FROM OrderItem oi " +
           "JOIN OrderDetail od ON oi.orderId = od.code " +
           "JOIN Product p ON oi.productCode = p.code " +
           "LEFT JOIN Category c ON p.category = c " +
           "GROUP BY c.name " +
           "ORDER BY totalRevenue DESC")
    List<Object[]> getSalesByCategory();
}

