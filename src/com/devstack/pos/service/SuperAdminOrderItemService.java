package com.devstack.pos.service;

import com.devstack.pos.entity.SuperAdminOrderItem;
import com.devstack.pos.repository.SuperAdminOrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}

