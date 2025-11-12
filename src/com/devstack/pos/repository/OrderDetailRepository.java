package com.devstack.pos.repository;

import com.devstack.pos.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    
    @Query("SELECT o FROM OrderDetail o WHERE o.customerId = :customerId ORDER BY o.issuedDate DESC")
    List<OrderDetail> findByCustomerId(@Param("customerId") Long customerId);
    
    // Get total amount spent by a customer (only PAID orders)
    @Query("SELECT COALESCE(SUM(o.totalCost), 0.0) FROM OrderDetail o WHERE o.customerId = :customerId AND o.paymentStatus = 'PAID'")
    Double getTotalSpentByCustomerId(@Param("customerId") Long customerId);
    
    @Query("SELECT o FROM OrderDetail o WHERE o.customerName LIKE %:search% ORDER BY o.issuedDate DESC")
    List<OrderDetail> findByCustomerNameContaining(@Param("search") String search);
    
    @Query("SELECT SUM(o.totalCost) FROM OrderDetail o WHERE o.paymentStatus = 'PAID'")
    Double getTotalRevenue();
    
    @Query("SELECT AVG(o.totalCost) FROM OrderDetail o WHERE o.paymentStatus = 'PAID'")
    Double getAverageOrderValue();
    
    @Query("SELECT COUNT(o) FROM OrderDetail o")
    Long getTotalOrderCount();
    
    @Query("SELECT SUM(o.totalCost) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate AND o.paymentStatus = 'PAID'")
    Double getRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o.customerName, SUM(o.totalCost) as total FROM OrderDetail o WHERE o.customerName IS NOT NULL AND o.paymentStatus = 'PAID' GROUP BY o.customerName ORDER BY total DESC")
    List<Object[]> getTopCustomersByRevenue();
    
    @Query("SELECT o FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
    List<OrderDetail> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Sales by cashier/operator (COUNT includes all orders, SUM only PAID orders)
    @Query("SELECT o.operatorEmail, COUNT(o), SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) FROM OrderDetail o GROUP BY o.operatorEmail ORDER BY SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) DESC")
    List<Object[]> getSalesByCashier();
    
    @Query("SELECT o.operatorEmail, COUNT(o), SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate GROUP BY o.operatorEmail ORDER BY SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) DESC")
    List<Object[]> getSalesByCashierByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Top customers with order count (COUNT includes all orders, SUM only PAID orders)
    @Query("SELECT o.customerName, COUNT(o), SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) FROM OrderDetail o WHERE o.customerName IS NOT NULL GROUP BY o.customerName ORDER BY SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) DESC")
    List<Object[]> getTopCustomersWithOrderCount();
    
    @Query("SELECT o.customerName, COUNT(o), SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate AND o.customerName IS NOT NULL GROUP BY o.customerName ORDER BY SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) DESC")
    List<Object[]> getTopCustomersWithOrderCountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Count orders by date range (all orders, including pending payments)
    @Query("SELECT COUNT(o) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
    Long countOrdersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Average order value by date range (only PAID orders)
    @Query("SELECT AVG(o.totalCost) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate AND o.paymentStatus = 'PAID'")
    Double getAverageOrderValueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find all pending payments
    @Query("SELECT o FROM OrderDetail o WHERE o.paymentStatus = 'PENDING' ORDER BY o.issuedDate ASC")
    List<OrderDetail> findPendingPayments();
    
    // Find pending payments by payment method
    @Query("SELECT o FROM OrderDetail o WHERE o.paymentStatus = 'PENDING' AND o.paymentMethod = :paymentMethod ORDER BY o.issuedDate ASC")
    List<OrderDetail> findPendingPaymentsByMethod(@Param("paymentMethod") String paymentMethod);
    
    // Get pending payments total by customer (all time)
    @Query("SELECT o.customerName, COALESCE(SUM(o.totalCost), 0.0) FROM OrderDetail o WHERE o.paymentStatus = 'PENDING' AND o.customerName IS NOT NULL GROUP BY o.customerName")
    List<Object[]> getPendingPaymentsByCustomer();
    
    // Get pending payments total by customer (with date range)
    @Query("SELECT o.customerName, COALESCE(SUM(o.totalCost), 0.0) FROM OrderDetail o WHERE o.paymentStatus = 'PENDING' AND o.customerName IS NOT NULL AND o.issuedDate BETWEEN :startDate AND :endDate GROUP BY o.customerName")
    List<Object[]> getPendingPaymentsByCustomerByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

