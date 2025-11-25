package com.devstack.pos.service;

import com.devstack.pos.entity.SuperAdminOrderDetail;
import com.devstack.pos.repository.SuperAdminOrderDetailRepository;
import com.devstack.pos.util.UserSessionData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SuperAdminOrderDetailService {
    
    private final SuperAdminOrderDetailRepository superAdminOrderDetailRepository;
    
    /**
     * Save a super admin order detail - only accessible by Super Admin
     */
    public SuperAdminOrderDetail saveSuperAdminOrderDetail(SuperAdminOrderDetail orderDetail) {
        if (!UserSessionData.isSuperAdmin()) {
            throw new SecurityException("Access Denied: Super Admin orders can only be managed by Super Admin users.");
        }
        return superAdminOrderDetailRepository.save(orderDetail);
    }
    
    /**
     * Find a super admin order detail by code - only accessible by Super Admin
     */
    public SuperAdminOrderDetail findSuperAdminOrderDetail(Long code) {
        if (!UserSessionData.isSuperAdmin()) {
            throw new SecurityException("Access Denied: Super Admin orders can only be accessed by Super Admin users.");
        }
        return superAdminOrderDetailRepository.findById(code).orElse(null);
    }
    
    /**
     * Update a super admin order detail - only accessible by Super Admin
     */
    public boolean updateSuperAdminOrderDetail(SuperAdminOrderDetail orderDetail) {
        if (!UserSessionData.isSuperAdmin()) {
            throw new SecurityException("Access Denied: Super Admin orders can only be managed by Super Admin users.");
        }
        if (superAdminOrderDetailRepository.existsById(orderDetail.getCode())) {
            superAdminOrderDetailRepository.save(orderDetail);
            return true;
        }
        return false;
    }
    
    /**
     * Delete a super admin order detail - only accessible by Super Admin
     */
    public boolean deleteSuperAdminOrderDetail(Long code) {
        if (!UserSessionData.isSuperAdmin()) {
            throw new SecurityException("Access Denied: Super Admin orders can only be managed by Super Admin users.");
        }
        if (superAdminOrderDetailRepository.existsById(code)) {
            superAdminOrderDetailRepository.deleteById(code);
            return true;
        }
        return false;
    }
    
    /**
     * Find all super admin order details - only accessible by Super Admin
     * Returns empty list for non-super admin users
     */
    @Transactional(readOnly = true)
    public List<SuperAdminOrderDetail> findAllSuperAdminOrderDetails() {
        if (!UserSessionData.isSuperAdmin()) {
            // Return empty list instead of throwing exception to prevent crashes
            return new ArrayList<>();
        }
        return superAdminOrderDetailRepository.findAll();
    }
    
    // Separate calculation methods for Super Admin orders - don't use existing methods
    // All methods return 0 or empty for non-super admin users
    
    @Transactional(readOnly = true)
    public Double getSuperAdminTotalRevenue() {
        if (!UserSessionData.isSuperAdmin()) {
            return 0.0;
        }
        Double revenue = superAdminOrderDetailRepository.getSuperAdminTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getSuperAdminRevenueByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        if (!UserSessionData.isSuperAdmin()) {
            return 0.0;
        }
        Double revenue = superAdminOrderDetailRepository.getSuperAdminRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Long getSuperAdminTotalOrderCount() {
        if (!UserSessionData.isSuperAdmin()) {
            return 0L;
        }
        Long count = superAdminOrderDetailRepository.getSuperAdminTotalOrderCount();
        return count != null ? count : 0L;
    }
    
    @Transactional(readOnly = true)
    public Long countSuperAdminOrdersByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        if (!UserSessionData.isSuperAdmin()) {
            return 0L;
        }
        Long count = superAdminOrderDetailRepository.countSuperAdminOrdersByDateRange(startDate, endDate);
        return count != null ? count : 0L;
    }
    
    @Transactional(readOnly = true)
    public Double getSuperAdminAverageOrderValue() {
        if (!UserSessionData.isSuperAdmin()) {
            return 0.0;
        }
        Double avg = superAdminOrderDetailRepository.getSuperAdminAverageOrderValue();
        return avg != null ? avg : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getSuperAdminAverageOrderValueByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        if (!UserSessionData.isSuperAdmin()) {
            return 0.0;
        }
        Double avg = superAdminOrderDetailRepository.getSuperAdminAverageOrderValueByDateRange(startDate, endDate);
        return avg != null ? avg : 0.0;
    }
    
    @Transactional(readOnly = true)
    public List<SuperAdminOrderDetail> findSuperAdminOrdersByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        if (!UserSessionData.isSuperAdmin()) {
            // Return empty list instead of throwing exception to prevent crashes
            return new ArrayList<>();
        }
        return superAdminOrderDetailRepository.findSuperAdminOrdersByDateRange(startDate, endDate);
    }
}

