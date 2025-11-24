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
}

