package com.devstack.pos.service;

import com.devstack.pos.entity.SuperAdminOrderItem;
import com.devstack.pos.repository.SuperAdminOrderItemRepository;
import com.devstack.pos.util.UserSessionData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SuperAdminOrderItemService {
    
    private final SuperAdminOrderItemRepository superAdminOrderItemRepository;
    
    /**
     * Save a super admin order item - only accessible by Super Admin
     */
    public SuperAdminOrderItem saveSuperAdminOrderItem(SuperAdminOrderItem orderItem) {
        if (!UserSessionData.isSuperAdmin()) {
            throw new SecurityException("Access Denied: Super Admin order items can only be managed by Super Admin users.");
        }
        return superAdminOrderItemRepository.save(orderItem);
    }
    
    /**
     * Save multiple super admin order items - only accessible by Super Admin
     */
    public List<SuperAdminOrderItem> saveAllSuperAdminOrderItems(List<SuperAdminOrderItem> orderItems) {
        if (!UserSessionData.isSuperAdmin()) {
            throw new SecurityException("Access Denied: Super Admin order items can only be managed by Super Admin users.");
        }
        return superAdminOrderItemRepository.saveAll(orderItems);
    }
    
    /**
     * Find all items for a specific super admin order - only accessible by Super Admin
     * Returns empty list for non-super admin users
     */
    @Transactional(readOnly = true)
    public List<SuperAdminOrderItem> findByOrderId(Long orderId) {
        if (!UserSessionData.isSuperAdmin()) {
            // Return empty list instead of throwing exception to prevent crashes
            return new ArrayList<>();
        }
        return superAdminOrderItemRepository.findByOrderId(orderId);
    }
    
    /**
     * Find a super admin order item by ID - only accessible by Super Admin
     */
    @Transactional(readOnly = true)
    public SuperAdminOrderItem findById(Long id) {
        if (!UserSessionData.isSuperAdmin()) {
            throw new SecurityException("Access Denied: Super Admin order items can only be accessed by Super Admin users.");
        }
        return superAdminOrderItemRepository.findById(id).orElse(null);
    }
    
    /**
     * Delete a super admin order item - only accessible by Super Admin
     */
    public void deleteSuperAdminOrderItem(Long id) {
        if (!UserSessionData.isSuperAdmin()) {
            throw new SecurityException("Access Denied: Super Admin order items can only be managed by Super Admin users.");
        }
        superAdminOrderItemRepository.deleteById(id);
    }
    
    // Separate calculation methods for General Items - don't use existing methods
    // All methods return 0 or empty for non-super admin users
    
    /**
     * Get total revenue from general items only (all time, only PAID orders)
     * Only accessible by Super Admin
     */
    @Transactional(readOnly = true)
    public Double getGeneralItemsTotalRevenue() {
        if (!UserSessionData.isSuperAdmin()) {
            return 0.0;
        }
        Double revenue = superAdminOrderItemRepository.getGeneralItemsTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }
    
    /**
     * Get total revenue from general items by date range (only PAID orders)
     * Only accessible by Super Admin
     */
    @Transactional(readOnly = true)
    public Double getGeneralItemsRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (!UserSessionData.isSuperAdmin()) {
            return 0.0;
        }
        Double revenue = superAdminOrderItemRepository.getGeneralItemsRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : 0.0;
    }
    
    /**
     * Get total quantity of general items sold (all time)
     * Only accessible by Super Admin
     */
    @Transactional(readOnly = true)
    public Double getGeneralItemsTotalQuantity() {
        if (!UserSessionData.isSuperAdmin()) {
            return 0.0;
        }
        Double quantity = superAdminOrderItemRepository.getGeneralItemsTotalQuantity();
        return quantity != null ? quantity : 0.0;
    }
    
    /**
     * Get total quantity of general items sold by date range
     * Only accessible by Super Admin
     */
    @Transactional(readOnly = true)
    public Double getGeneralItemsQuantityByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (!UserSessionData.isSuperAdmin()) {
            return 0.0;
        }
        Double quantity = superAdminOrderItemRepository.getGeneralItemsQuantityByDateRange(startDate, endDate);
        return quantity != null ? quantity : 0.0;
    }
    
    /**
     * Get count of orders containing general items (all time)
     * Only accessible by Super Admin
     */
    @Transactional(readOnly = true)
    public Long getGeneralItemsOrderCount() {
        if (!UserSessionData.isSuperAdmin()) {
            return 0L;
        }
        Long count = superAdminOrderItemRepository.getGeneralItemsOrderCount();
        return count != null ? count : 0L;
    }
    
    /**
     * Get count of orders containing general items by date range
     * Only accessible by Super Admin
     */
    @Transactional(readOnly = true)
    public Long getGeneralItemsOrderCountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (!UserSessionData.isSuperAdmin()) {
            return 0L;
        }
        Long count = superAdminOrderItemRepository.getGeneralItemsOrderCountByDateRange(startDate, endDate);
        return count != null ? count : 0L;
    }
}

