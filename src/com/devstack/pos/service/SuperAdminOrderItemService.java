package com.devstack.pos.service;

import com.devstack.pos.entity.SuperAdminOrderItem;
import com.devstack.pos.repository.SuperAdminOrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SuperAdminOrderItemService {
    
    private final SuperAdminOrderItemRepository superAdminOrderItemRepository;
    
    /**
     * Save a super admin order item
     */
    public SuperAdminOrderItem saveSuperAdminOrderItem(SuperAdminOrderItem orderItem) {
        return superAdminOrderItemRepository.save(orderItem);
    }
    
    /**
     * Save multiple super admin order items
     */
    public List<SuperAdminOrderItem> saveAllSuperAdminOrderItems(List<SuperAdminOrderItem> orderItems) {
        return superAdminOrderItemRepository.saveAll(orderItems);
    }
    
    /**
     * Find all items for a specific super admin order
     */
    @Transactional(readOnly = true)
    public List<SuperAdminOrderItem> findByOrderId(Long orderId) {
        return superAdminOrderItemRepository.findByOrderId(orderId);
    }
    
    /**
     * Find a super admin order item by ID
     */
    @Transactional(readOnly = true)
    public SuperAdminOrderItem findById(Long id) {
        return superAdminOrderItemRepository.findById(id).orElse(null);
    }
    
    /**
     * Delete a super admin order item
     */
    public void deleteSuperAdminOrderItem(Long id) {
        superAdminOrderItemRepository.deleteById(id);
    }
    
    // Separate calculation methods for General Items - don't use existing methods
    
    /**
     * Get total revenue from general items only (all time, only PAID orders)
     */
    @Transactional(readOnly = true)
    public Double getGeneralItemsTotalRevenue() {
        Double revenue = superAdminOrderItemRepository.getGeneralItemsTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }
    
    /**
     * Get total revenue from general items by date range (only PAID orders)
     */
    @Transactional(readOnly = true)
    public Double getGeneralItemsRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double revenue = superAdminOrderItemRepository.getGeneralItemsRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : 0.0;
    }
    
    /**
     * Get total quantity of general items sold (all time)
     */
    @Transactional(readOnly = true)
    public Double getGeneralItemsTotalQuantity() {
        Double quantity = superAdminOrderItemRepository.getGeneralItemsTotalQuantity();
        return quantity != null ? quantity : 0.0;
    }
    
    /**
     * Get total quantity of general items sold by date range
     */
    @Transactional(readOnly = true)
    public Double getGeneralItemsQuantityByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double quantity = superAdminOrderItemRepository.getGeneralItemsQuantityByDateRange(startDate, endDate);
        return quantity != null ? quantity : 0.0;
    }
    
    /**
     * Get count of orders containing general items (all time)
     */
    @Transactional(readOnly = true)
    public Long getGeneralItemsOrderCount() {
        Long count = superAdminOrderItemRepository.getGeneralItemsOrderCount();
        return count != null ? count : 0L;
    }
    
    /**
     * Get count of orders containing general items by date range
     */
    @Transactional(readOnly = true)
    public Long getGeneralItemsOrderCountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Long count = superAdminOrderItemRepository.getGeneralItemsOrderCountByDateRange(startDate, endDate);
        return count != null ? count : 0L;
    }
}

