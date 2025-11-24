package com.devstack.pos.service;

import com.devstack.pos.entity.SuperAdminOrderDetail;
import com.devstack.pos.repository.SuperAdminOrderDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SuperAdminOrderDetailService {
    
    private final SuperAdminOrderDetailRepository superAdminOrderDetailRepository;
    
    public SuperAdminOrderDetail saveSuperAdminOrderDetail(SuperAdminOrderDetail orderDetail) {
        return superAdminOrderDetailRepository.save(orderDetail);
    }
    
    public SuperAdminOrderDetail findSuperAdminOrderDetail(Long code) {
        return superAdminOrderDetailRepository.findById(code).orElse(null);
    }
    
    public boolean updateSuperAdminOrderDetail(SuperAdminOrderDetail orderDetail) {
        if (superAdminOrderDetailRepository.existsById(orderDetail.getCode())) {
            superAdminOrderDetailRepository.save(orderDetail);
            return true;
        }
        return false;
    }
    
    public boolean deleteSuperAdminOrderDetail(Long code) {
        if (superAdminOrderDetailRepository.existsById(code)) {
            superAdminOrderDetailRepository.deleteById(code);
            return true;
        }
        return false;
    }
    
    @Transactional(readOnly = true)
    public java.util.List<SuperAdminOrderDetail> findAllSuperAdminOrderDetails() {
        return superAdminOrderDetailRepository.findAll();
    }
    
    // Separate calculation methods for Super Admin orders - don't use existing methods
    
    @Transactional(readOnly = true)
    public Double getSuperAdminTotalRevenue() {
        Double revenue = superAdminOrderDetailRepository.getSuperAdminTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getSuperAdminRevenueByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        Double revenue = superAdminOrderDetailRepository.getSuperAdminRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Long getSuperAdminTotalOrderCount() {
        Long count = superAdminOrderDetailRepository.getSuperAdminTotalOrderCount();
        return count != null ? count : 0L;
    }
    
    @Transactional(readOnly = true)
    public Long countSuperAdminOrdersByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        Long count = superAdminOrderDetailRepository.countSuperAdminOrdersByDateRange(startDate, endDate);
        return count != null ? count : 0L;
    }
    
    @Transactional(readOnly = true)
    public Double getSuperAdminAverageOrderValue() {
        Double avg = superAdminOrderDetailRepository.getSuperAdminAverageOrderValue();
        return avg != null ? avg : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getSuperAdminAverageOrderValueByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        Double avg = superAdminOrderDetailRepository.getSuperAdminAverageOrderValueByDateRange(startDate, endDate);
        return avg != null ? avg : 0.0;
    }
    
    @Transactional(readOnly = true)
    public java.util.List<SuperAdminOrderDetail> findSuperAdminOrdersByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        return superAdminOrderDetailRepository.findSuperAdminOrdersByDateRange(startDate, endDate);
    }
}

