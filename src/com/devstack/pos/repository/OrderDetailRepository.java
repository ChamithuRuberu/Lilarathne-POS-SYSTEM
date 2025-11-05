package com.devstack.pos.repository;

import com.devstack.pos.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    
    @Query("SELECT o FROM OrderDetail o WHERE o.customerId = :customerId ORDER BY o.issuedDate DESC")
    List<OrderDetail> findByCustomerId(@Param("customerId") Long customerId);
    
    // Get total amount spent by a customer
    @Query("SELECT COALESCE(SUM(o.totalCost), 0.0) FROM OrderDetail o WHERE o.customerId = :customerId")
    Double getTotalSpentByCustomerId(@Param("customerId") Long customerId);
    
    @Query("SELECT o FROM OrderDetail o WHERE o.customerName LIKE %:search% ORDER BY o.issuedDate DESC")
    List<OrderDetail> findByCustomerNameContaining(@Param("search") String search);
    
    @Query("SELECT SUM(o.totalCost) FROM OrderDetail o")
    Double getTotalRevenue();
    
    @Query("SELECT AVG(o.totalCost) FROM OrderDetail o")
    Double getAverageOrderValue();
    
    @Query("SELECT COUNT(o) FROM OrderDetail o")
    Long getTotalOrderCount();
    
    @Query("SELECT SUM(o.totalCost) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
    Double getRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o.customerName, SUM(o.totalCost) as total FROM OrderDetail o WHERE o.customerName IS NOT NULL GROUP BY o.customerName ORDER BY total DESC")
    List<Object[]> getTopCustomersByRevenue();
    
    @Query("SELECT o FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
    List<OrderDetail> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Sales by cashier/operator
    @Query("SELECT o.operatorEmail, COUNT(o), SUM(o.totalCost) FROM OrderDetail o GROUP BY o.operatorEmail ORDER BY SUM(o.totalCost) DESC")
    List<Object[]> getSalesByCashier();
    
    @Query("SELECT o.operatorEmail, COUNT(o), SUM(o.totalCost) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate GROUP BY o.operatorEmail ORDER BY SUM(o.totalCost) DESC")
    List<Object[]> getSalesByCashierByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Top customers with order count
    @Query("SELECT o.customerName, COUNT(o), SUM(o.totalCost) FROM OrderDetail o WHERE o.customerName IS NOT NULL GROUP BY o.customerName ORDER BY SUM(o.totalCost) DESC")
    List<Object[]> getTopCustomersWithOrderCount();
    
    @Query("SELECT o.customerName, COUNT(o), SUM(o.totalCost) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate AND o.customerName IS NOT NULL GROUP BY o.customerName ORDER BY SUM(o.totalCost) DESC")
    List<Object[]> getTopCustomersWithOrderCountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Count orders by date range
    @Query("SELECT COUNT(o) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
    Long countOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Average order value by date range
    @Query("SELECT AVG(o.totalCost) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
    Double getAverageOrderValueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

