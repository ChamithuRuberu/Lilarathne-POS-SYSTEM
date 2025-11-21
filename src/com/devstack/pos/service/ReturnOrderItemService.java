package com.devstack.pos.service;

import com.devstack.pos.entity.ReturnOrderItem;
import com.devstack.pos.repository.ReturnOrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnOrderItemService {
    
    private final ReturnOrderItemRepository returnOrderItemRepository;
    
    /**
     * Save a return order item
     */
    public ReturnOrderItem saveReturnOrderItem(ReturnOrderItem item) {
        return returnOrderItemRepository.save(item);
    }
    
    /**
     * Save multiple return order items
     */
    public List<ReturnOrderItem> saveAllReturnOrderItems(List<ReturnOrderItem> items) {
        return returnOrderItemRepository.saveAll(items);
    }
    
    /**
     * Find a return order item by ID
     */
    @Transactional(readOnly = true)
    public Optional<ReturnOrderItem> findById(Long id) {
        return returnOrderItemRepository.findById(id);
    }
    
    /**
     * Find all items for a specific return order
     */
    @Transactional(readOnly = true)
    public List<ReturnOrderItem> findByReturnOrderId(Integer returnOrderId) {
        return returnOrderItemRepository.findByReturnOrderId(returnOrderId);
    }
    
    /**
     * Find all returns for a specific product
     */
    @Transactional(readOnly = true)
    public List<ReturnOrderItem> findByProductCode(Integer productCode) {
        return returnOrderItemRepository.findByProductCode(productCode);
    }
    
    /**
     * Find all returns from a specific batch
     */
    @Transactional(readOnly = true)
    public List<ReturnOrderItem> findByBatchCode(String batchCode) {
        return returnOrderItemRepository.findByBatchCode(batchCode);
    }
    
    /**
     * Find returns for a specific order item
     */
    @Transactional(readOnly = true)
    public List<ReturnOrderItem> findByOrderItemId(Long orderItemId) {
        return returnOrderItemRepository.findByOrderItemId(orderItemId);
    }
    
    /**
     * Get total quantity returned for a product (supports decimal quantities)
     */
    @Transactional(readOnly = true)
    public Double getTotalQuantityReturnedByProduct(Integer productCode) {
        Double result = returnOrderItemRepository.getTotalQuantityReturnedByProduct(productCode);
        return result != null ? result : 0.0;
    }
    
    /**
     * Get total refund amount for a product
     */
    @Transactional(readOnly = true)
    public Double getTotalRefundByProduct(Integer productCode) {
        return returnOrderItemRepository.getTotalRefundByProduct(productCode);
    }
    
    /**
     * Get total refund amount for a product within date range
     */
    @Transactional(readOnly = true)
    public Double getTotalRefundByProductAndDateRange(Integer productCode, LocalDateTime startDate, LocalDateTime endDate) {
        return returnOrderItemRepository.getTotalRefundByProductAndDateRange(productCode, startDate, endDate);
    }
    
    /**
     * Get refund amounts and quantities grouped by product code within date range
     * Returns list of Object[]: [productCode, totalRefundAmount, totalReturnedQuantity]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getRefundsAndQuantitiesByProductByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return returnOrderItemRepository.getRefundsAndQuantitiesByProductByDateRange(startDate, endDate);
    }
    
    /**
     * Get refund amounts and quantities grouped by product code (all time)
     * Returns list of Object[]: [productCode, totalRefundAmount, totalReturnedQuantity]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getRefundsAndQuantitiesByProduct() {
        return returnOrderItemRepository.getRefundsAndQuantitiesByProduct();
    }
    
    /**
     * Get refund amounts grouped by product code within date range
     * Returns list of Object[]: [productCode, totalRefundAmount]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getRefundsByProductByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return returnOrderItemRepository.getRefundsByProductByDateRange(startDate, endDate);
    }
    
    /**
     * Get refund amounts grouped by product code (all time)
     * Returns list of Object[]: [productCode, totalRefundAmount]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getRefundsByProduct() {
        return returnOrderItemRepository.getRefundsByProduct();
    }
    
    /**
     * Count items in a return order
     */
    @Transactional(readOnly = true)
    public Long countItemsInReturnOrder(Integer returnOrderId) {
        return returnOrderItemRepository.countItemsInReturnOrder(returnOrderId);
    }
    
    /**
     * Find items where inventory not yet restored
     */
    @Transactional(readOnly = true)
    public List<ReturnOrderItem> findUnrestoredItems() {
        return returnOrderItemRepository.findUnrestoredItems();
    }
    
    /**
     * Update an item
     */
    public ReturnOrderItem updateReturnOrderItem(ReturnOrderItem item) {
        return returnOrderItemRepository.save(item);
    }
    
    /**
     * Get refund amounts grouped by category within date range
     * Returns list of Object[]: [categoryName, totalRefundAmount]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getRefundsByCategoryByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return returnOrderItemRepository.getRefundsByCategoryByDateRange(startDate, endDate);
    }
    
    /**
     * Get refund amounts grouped by category (all time)
     * Returns list of Object[]: [categoryName, totalRefundAmount]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getRefundsByCategory() {
        return returnOrderItemRepository.getRefundsByCategory();
    }
    
    /**
     * Get refund amounts grouped by customer within date range
     * Returns list of Object[]: [customerEmail, totalRefundAmount]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getRefundsByCustomerByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return returnOrderItemRepository.getRefundsByCustomerByDateRange(startDate, endDate);
    }
    
    /**
     * Get refund amounts grouped by customer (all time)
     * Returns list of Object[]: [customerEmail, totalRefundAmount]
     */
    @Transactional(readOnly = true)
    public List<Object[]> getRefundsByCustomer() {
        return returnOrderItemRepository.getRefundsByCustomer();
    }
    
    /**
     * Delete a return order item
     */
    public void deleteReturnOrderItem(Long id) {
        returnOrderItemRepository.deleteById(id);
    }
}

