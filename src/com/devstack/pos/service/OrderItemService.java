package com.devstack.pos.service;

import com.devstack.pos.entity.OrderItem;
import com.devstack.pos.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderItemService {
    
    private final OrderItemRepository orderItemRepository;
    
    /**
     * Save an order item
     */
    public OrderItem saveOrderItem(OrderItem orderItem) {
        return orderItemRepository.save(orderItem);
    }
    
    /**
     * Save multiple order items
     */
    public List<OrderItem> saveAllOrderItems(List<OrderItem> orderItems) {
        return orderItemRepository.saveAll(orderItems);
    }
    
    /**
     * Find an order item by ID
     */
    @Transactional(readOnly = true)
    public Optional<OrderItem> findById(Long id) {
        return orderItemRepository.findById(id);
    }
    
    /**
     * Find all items for a specific order
     */
    @Transactional(readOnly = true)
    public List<OrderItem> findByOrderId(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }
    
    /**
     * Find all items for a specific product
     */
    @Transactional(readOnly = true)
    public List<OrderItem> findByProductCode(Integer productCode) {
        return orderItemRepository.findByProductCode(productCode);
    }
    
    /**
     * Find all items from a specific batch
     */
    @Transactional(readOnly = true)
    public List<OrderItem> findByBatchCode(String batchCode) {
        return orderItemRepository.findByBatchCode(batchCode);
    }
    
    /**
     * Get total quantity sold for a product
     */
    @Transactional(readOnly = true)
    public Integer getTotalQuantitySoldByProduct(Integer productCode) {
        return orderItemRepository.getTotalQuantitySoldByProduct(productCode);
    }
    
    /**
     * Get total revenue from a product
     */
    @Transactional(readOnly = true)
    public Double getTotalRevenueByProduct(Integer productCode) {
        return orderItemRepository.getTotalRevenueByProduct(productCode);
    }
    
    /**
     * Count items in an order
     */
    @Transactional(readOnly = true)
    public Long countItemsInOrder(Long orderId) {
        return orderItemRepository.countItemsInOrder(orderId);
    }
    
    /**
     * Get top selling products by quantity within a date range
     * Returns list of Object[]: [productCode, productName, totalQuantity]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getTopSellingProductsByQuantity(LocalDateTime startDate, LocalDateTime endDate) {
        return orderItemRepository.getTopSellingProductsByQuantity(startDate, endDate);
    }
    
    /**
     * Get top selling products by quantity (all time)
     * Returns list of Object[]: [productCode, productName, totalQuantity]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getTopSellingProductsByQuantity() {
        return orderItemRepository.getTopSellingProductsByQuantity();
    }
    
    /**
     * Get top selling products with revenue within a date range
     * Returns list of Object[]: [productCode, productName, totalQuantity, totalRevenue]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getTopSellingProductsWithRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        return orderItemRepository.getTopSellingProductsWithRevenue(startDate, endDate);
    }
    
    /**
     * Get top selling products with revenue (all time)
     * Returns list of Object[]: [productCode, productName, totalQuantity, totalRevenue]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getTopSellingProductsWithRevenue() {
        return orderItemRepository.getTopSellingProductsWithRevenue();
    }
    
    /**
     * Get sales by category within a date range
     * Returns list of Object[]: [categoryName, orderCount, totalRevenue]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getSalesByCategory(LocalDateTime startDate, LocalDateTime endDate) {
        return orderItemRepository.getSalesByCategory(startDate, endDate);
    }
    
    /**
     * Get sales by category (all time)
     * Returns list of Object[]: [categoryName, orderCount, totalRevenue]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getSalesByCategory() {
        return orderItemRepository.getSalesByCategory();
    }
    
    /**
     * Delete an order item
     */
    public void deleteOrderItem(Long id) {
        orderItemRepository.deleteById(id);
    }
    
    // Customer Purchase History Methods
    @Transactional(readOnly = true)
    public List<Object[]> getCustomerFavoriteProducts(Long customerId) {
        return orderItemRepository.getCustomerFavoriteProducts(customerId);
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getCustomerFavoriteCategories(Long customerId) {
        return orderItemRepository.getCustomerFavoriteCategories(customerId);
    }
    
    // Product Performance Analytics Methods
    @Transactional(readOnly = true)
    public List<Object[]> getProductSalesHistory(Integer productCode) {
        return orderItemRepository.getProductSalesHistory(productCode);
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getProductSalesHistoryByDateRange(Integer productCode, LocalDateTime startDate, LocalDateTime endDate) {
        return orderItemRepository.getProductSalesHistoryByDateRange(productCode, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getProductSalesTrendByDate(Integer productCode) {
        return orderItemRepository.getProductSalesTrendByDate(productCode);
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getProductSalesTrendByDateRange(Integer productCode, LocalDateTime startDate, LocalDateTime endDate) {
        return orderItemRepository.getProductSalesTrendByDateRange(productCode, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public Long getProductUniqueCustomers(Integer productCode) {
        Long count = orderItemRepository.getProductUniqueCustomers(productCode);
        return count != null ? count : 0L;
    }
    
    @Transactional(readOnly = true)
    public Long getProductUniqueCustomersByDateRange(Integer productCode, LocalDateTime startDate, LocalDateTime endDate) {
        Long count = orderItemRepository.getProductUniqueCustomersByDateRange(productCode, startDate, endDate);
        return count != null ? count : 0L;
    }
    
    @Transactional(readOnly = true)
    public Double getProductAverageQuantityPerOrder(Integer productCode) {
        Double avg = orderItemRepository.getProductAverageQuantityPerOrder(productCode);
        return avg != null ? avg : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getProductAverageQuantityPerOrderByDateRange(Integer productCode, LocalDateTime startDate, LocalDateTime endDate) {
        Double avg = orderItemRepository.getProductAverageQuantityPerOrderByDateRange(productCode, startDate, endDate);
        return avg != null ? avg : 0.0;
    }
}

