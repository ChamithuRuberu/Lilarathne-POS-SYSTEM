package com.devstack.pos.service;

import com.devstack.pos.entity.ReturnOrderItem;
import com.devstack.pos.repository.ReturnOrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Get total quantity returned for a product
     */
    @Transactional(readOnly = true)
    public Integer getTotalQuantityReturnedByProduct(Integer productCode) {
        return returnOrderItemRepository.getTotalQuantityReturnedByProduct(productCode);
    }
    
    /**
     * Get total refund amount for a product
     */
    @Transactional(readOnly = true)
    public Double getTotalRefundByProduct(Integer productCode) {
        return returnOrderItemRepository.getTotalRefundByProduct(productCode);
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
     * Delete a return order item
     */
    public void deleteReturnOrderItem(Long id) {
        returnOrderItemRepository.deleteById(id);
    }
}

