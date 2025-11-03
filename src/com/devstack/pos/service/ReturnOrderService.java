package com.devstack.pos.service;

import com.devstack.pos.entity.ReturnOrder;
import com.devstack.pos.repository.ReturnOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnOrderService {
    
    private final ReturnOrderRepository returnOrderRepository;
    
    public ReturnOrder saveReturnOrder(ReturnOrder returnOrder) {
        return returnOrderRepository.save(returnOrder);
    }
    
    public ReturnOrder updateReturnOrder(ReturnOrder returnOrder) {
        return returnOrderRepository.save(returnOrder);
    }
    
    @Transactional(readOnly = true)
    public Optional<ReturnOrder> findById(Integer id) {
        return returnOrderRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<ReturnOrder> findByReturnId(String returnId) {
        return returnOrderRepository.findByReturnId(returnId);
    }
    
    @Transactional(readOnly = true)
    public List<ReturnOrder> findByOrderId(Integer orderId) {
        return returnOrderRepository.findByOrderId(orderId);
    }
    
    @Transactional(readOnly = true)
    public List<ReturnOrder> findByCustomerEmail(String customerEmail) {
        return returnOrderRepository.findByCustomerEmail(customerEmail);
    }
    
    @Transactional(readOnly = true)
    public List<ReturnOrder> findByStatus(String status) {
        return returnOrderRepository.findByStatus(status);
    }
    
    @Transactional(readOnly = true)
    public List<ReturnOrder> findAllReturnOrders() {
        return returnOrderRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<ReturnOrder> findByReturnDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return returnOrderRepository.findByReturnDateBetween(startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public List<ReturnOrder> findByStatusAndReturnDateBetween(String status, LocalDateTime startDate, LocalDateTime endDate) {
        return returnOrderRepository.findByStatusAndReturnDateBetween(status, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public List<ReturnOrder> searchReturnOrders(String returnId, String customerEmail, String status, 
                                                 LocalDateTime startDate, LocalDateTime endDate) {
        return returnOrderRepository.searchReturnOrders(returnId, customerEmail, status, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public Long countByStatus(String status) {
        Long count = returnOrderRepository.countByStatus(status);
        return count != null ? count : 0L;
    }
    
    @Transactional(readOnly = true)
    public Double getTotalRefundAmount() {
        Double total = returnOrderRepository.getTotalRefundAmount();
        return total != null ? total : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getTotalRefundAmountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double total = returnOrderRepository.getTotalRefundAmountByDateRange(startDate, endDate);
        return total != null ? total : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Long countReturnsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Long count = returnOrderRepository.countReturnsByDateRange(startDate, endDate);
        return count != null ? count : 0L;
    }
    
    public ReturnOrder approveReturn(Integer id, String approvedBy) {
        Optional<ReturnOrder> optionalReturn = returnOrderRepository.findById(id);
        if (optionalReturn.isPresent()) {
            ReturnOrder returnOrder = optionalReturn.get();
            returnOrder.setStatus("APPROVED");
            returnOrder.setApprovalDate(LocalDateTime.now());
            returnOrder.setProcessedBy(approvedBy);
            return returnOrderRepository.save(returnOrder);
        }
        throw new RuntimeException("Return order not found with id: " + id);
    }
    
    public ReturnOrder rejectReturn(Integer id, String rejectedBy) {
        Optional<ReturnOrder> optionalReturn = returnOrderRepository.findById(id);
        if (optionalReturn.isPresent()) {
            ReturnOrder returnOrder = optionalReturn.get();
            returnOrder.setStatus("REJECTED");
            returnOrder.setProcessedBy(rejectedBy);
            return returnOrderRepository.save(returnOrder);
        }
        throw new RuntimeException("Return order not found with id: " + id);
    }
    
    public ReturnOrder completeReturn(Integer id) {
        Optional<ReturnOrder> optionalReturn = returnOrderRepository.findById(id);
        if (optionalReturn.isPresent()) {
            ReturnOrder returnOrder = optionalReturn.get();
            returnOrder.setStatus("COMPLETED");
            returnOrder.setCompletionDate(LocalDateTime.now());
            return returnOrderRepository.save(returnOrder);
        }
        throw new RuntimeException("Return order not found with id: " + id);
    }
    
    public void deleteReturnOrder(Integer id) {
        returnOrderRepository.deleteById(id);
    }
}

