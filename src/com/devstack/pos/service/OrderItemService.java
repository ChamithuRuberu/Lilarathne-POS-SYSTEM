package com.devstack.pos.service;

import com.devstack.pos.entity.OrderItem;
import com.devstack.pos.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Delete an order item
     */
    public void deleteOrderItem(Long id) {
        orderItemRepository.deleteById(id);
    }
}

