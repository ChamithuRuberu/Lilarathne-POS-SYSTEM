package com.devstack.pos.service;

import com.devstack.pos.entity.GeneralItem;
import com.devstack.pos.repository.GeneralItemRepository;
import com.devstack.pos.util.UserSessionData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GeneralItemService {
    
    private final GeneralItemRepository generalItemRepository;
    
    /**
     * Save a general item - only accessible by Super Admin
     */
    public GeneralItem saveGeneralItem(GeneralItem generalItem) {
        if (!UserSessionData.isSuperAdmin()) {
            throw new SecurityException("Access Denied: General Items can only be managed by Super Admin users.");
        }
        return generalItemRepository.save(generalItem);
    }
    
    /**
     * Update a general item - only accessible by Super Admin
     */
    public boolean updateGeneralItem(GeneralItem generalItem) {
        if (!UserSessionData.isSuperAdmin()) {
            throw new SecurityException("Access Denied: General Items can only be managed by Super Admin users.");
        }
        if (generalItemRepository.existsById(generalItem.getId())) {
            generalItemRepository.save(generalItem);
            return true;
        }
        return false;
    }
    
    /**
     * Delete a general item - only accessible by Super Admin
     */
    public boolean deleteGeneralItem(Long id) {
        if (!UserSessionData.isSuperAdmin()) {
            throw new SecurityException("Access Denied: General Items can only be managed by Super Admin users.");
        }
        if (generalItemRepository.existsById(id)) {
            generalItemRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    /**
     * Find a general item by ID - only accessible by Super Admin
     */
    public GeneralItem findGeneralItem(Long id) {
        if (!UserSessionData.isSuperAdmin()) {
            throw new SecurityException("Access Denied: General Items can only be accessed by Super Admin users.");
        }
        return generalItemRepository.findById(id).orElse(null);
    }
    
    /**
     * Find all general items - only accessible by Super Admin
     * Returns empty list for non-super admin users
     */
    public List<GeneralItem> findAllGeneralItems() {
        if (!UserSessionData.isSuperAdmin()) {
            // Return empty list instead of throwing exception to prevent crashes
            return new ArrayList<>();
        }
        return generalItemRepository.findAllByOrderByNameAsc();
    }
}

