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
    
    @Query("SELECT o FROM OrderDetail o WHERE o.customerEmail = :email")
    List<OrderDetail> findByCustomerEmail(@Param("email") String email);
    
    @Query("SELECT SUM(o.totalCost) FROM OrderDetail o")
    Double getTotalRevenue();
    
    @Query("SELECT AVG(o.totalCost) FROM OrderDetail o")
    Double getAverageOrderValue();
    
    @Query("SELECT COUNT(o) FROM OrderDetail o")
    Long getTotalOrderCount();
    
    @Query("SELECT SUM(o.totalCost) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
    Double getRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o.customerEmail, SUM(o.totalCost) as total FROM OrderDetail o GROUP BY o.customerEmail ORDER BY total DESC")
    List<Object[]> getTopCustomersByRevenue();
    
    @Query("SELECT o FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
    List<OrderDetail> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Sales by cashier/operator
    @Query("SELECT o.operatorEmail, COUNT(o), SUM(o.totalCost) FROM OrderDetail o GROUP BY o.operatorEmail ORDER BY SUM(o.totalCost) DESC")
    List<Object[]> getSalesByCashier();
    
    @Query("SELECT o.operatorEmail, COUNT(o), SUM(o.totalCost) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate GROUP BY o.operatorEmail ORDER BY SUM(o.totalCost) DESC")
    List<Object[]> getSalesByCashierByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Top customers with order count
    @Query("SELECT o.customerEmail, COUNT(o), SUM(o.totalCost) FROM OrderDetail o GROUP BY o.customerEmail ORDER BY SUM(o.totalCost) DESC")
    List<Object[]> getTopCustomersWithOrderCount();
    
    @Query("SELECT o.customerEmail, COUNT(o), SUM(o.totalCost) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate GROUP BY o.customerEmail ORDER BY SUM(o.totalCost) DESC")
    List<Object[]> getTopCustomersWithOrderCountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Count orders by date range
    @Query("SELECT COUNT(o) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
    Long countOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Average order value by date range
    @Query("SELECT AVG(o.totalCost) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
    Double getAverageOrderValueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

