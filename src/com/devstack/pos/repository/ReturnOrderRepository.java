package com.devstack.pos.repository;

import com.devstack.pos.entity.ReturnOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnOrderRepository extends JpaRepository<ReturnOrder, Integer> {
    
    Optional<ReturnOrder> findByReturnId(String returnId);
    
    List<ReturnOrder> findByOrderId(Integer orderId);
    
    List<ReturnOrder> findByCustomerEmail(String customerEmail);
    
    List<ReturnOrder> findByStatus(String status);
    
    @Query("SELECT r FROM ReturnOrder r WHERE r.returnDate BETWEEN :startDate AND :endDate")
    List<ReturnOrder> findByReturnDateBetween(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT r FROM ReturnOrder r WHERE r.status = :status AND r.returnDate BETWEEN :startDate AND :endDate")
    List<ReturnOrder> findByStatusAndReturnDateBetween(@Param("status") String status,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT r FROM ReturnOrder r WHERE " +
           "(:returnId IS NULL OR r.returnId LIKE %:returnId%) AND " +
           "(:customerEmail IS NULL OR r.customerEmail LIKE %:customerEmail%) AND " +
           "(:status IS NULL OR :status = 'All' OR r.status = :status) AND " +
           "r.returnDate BETWEEN :startDate AND :endDate " +
           "ORDER BY r.returnDate DESC")
    List<ReturnOrder> searchReturnOrders(@Param("returnId") String returnId,
                                          @Param("customerEmail") String customerEmail,
                                          @Param("status") String status,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(r) FROM ReturnOrder r WHERE r.status = :status")
    Long countByStatus(@Param("status") String status);
    
    @Query("SELECT SUM(r.refundAmount) FROM ReturnOrder r WHERE r.status = 'COMPLETED'")
    Double getTotalRefundAmount();
    
    @Query("SELECT SUM(r.refundAmount) FROM ReturnOrder r WHERE r.status = 'COMPLETED' AND r.returnDate BETWEEN :startDate AND :endDate")
    Double getTotalRefundAmountByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(r) FROM ReturnOrder r WHERE r.returnDate BETWEEN :startDate AND :endDate")
    Long countReturnsByDateRange(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
}

