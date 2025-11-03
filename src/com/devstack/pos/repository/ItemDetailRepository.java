package com.devstack.pos.repository;

import com.devstack.pos.entity.ItemDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ItemDetailRepository extends JpaRepository<ItemDetail, Long> {
    List<ItemDetail> findByOrderCode(Integer orderCode);
    List<ItemDetail> findByDetailCode(String detailCode);
    
    // Top selling products by quantity
    @Query("SELECT i.detailCode, SUM(i.qty), SUM(i.amount) FROM ItemDetail i GROUP BY i.detailCode ORDER BY SUM(i.qty) DESC")
    List<Object[]> getTopSellingProducts();
    
    // Top selling products by revenue
    @Query("SELECT i.detailCode, SUM(i.qty), SUM(i.amount) FROM ItemDetail i GROUP BY i.detailCode ORDER BY SUM(i.amount) DESC")
    List<Object[]> getTopSellingProductsByRevenue();
    
    // Total profit calculation (requires joining with ProductDetail for buying price)
    @Query("SELECT SUM(i.amount) FROM ItemDetail i")
    Double getTotalRevenue();
    
    // Sales by product category (using product code as category)
    @Query("SELECT pd.productCode, SUM(i.qty), SUM(i.amount) " +
           "FROM ItemDetail i JOIN ProductDetail pd ON i.detailCode = pd.code " +
           "GROUP BY pd.productCode ORDER BY SUM(i.amount) DESC")
    List<Object[]> getSalesByCategory();
    
    // Profit calculation by product
    @Query("SELECT pd.code, p.description, SUM(i.qty), " +
           "SUM(i.qty * pd.buyingPrice) as cost, SUM(i.amount) as revenue, " +
           "(SUM(i.amount) - SUM(i.qty * pd.buyingPrice)) as profit " +
           "FROM ItemDetail i " +
           "JOIN ProductDetail pd ON i.detailCode = pd.code " +
           "JOIN Product p ON pd.productCode = p.code " +
           "GROUP BY pd.code, p.description " +
           "ORDER BY profit DESC")
    List<Object[]> getProfitByProduct();
    
    // Total profit calculation
    @Query("SELECT SUM(i.amount) - SUM(i.qty * pd.buyingPrice) " +
           "FROM ItemDetail i JOIN ProductDetail pd ON i.detailCode = pd.code")
    Double getTotalProfit();
    
    // Cost of goods sold
    @Query("SELECT SUM(i.qty * pd.buyingPrice) " +
           "FROM ItemDetail i JOIN ProductDetail pd ON i.detailCode = pd.code")
    Double getCostOfGoodsSold();
    
    // Items by date range (through order)
    @Query("SELECT i FROM ItemDetail i WHERE i.orderCode IN " +
           "(SELECT o.code FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate)")
    List<ItemDetail> findItemsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Profit by date range
    @Query("SELECT SUM(i.amount) - SUM(i.qty * pd.buyingPrice) " +
           "FROM ItemDetail i JOIN ProductDetail pd ON i.detailCode = pd.code " +
           "WHERE i.orderCode IN (SELECT o.code FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate)")
    Double getProfitByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Cost of goods by date range
    @Query("SELECT SUM(i.qty * pd.buyingPrice) " +
           "FROM ItemDetail i JOIN ProductDetail pd ON i.detailCode = pd.code " +
           "WHERE i.orderCode IN (SELECT o.code FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate)")
    Double getCostOfGoodsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

