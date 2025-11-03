package com.devstack.pos.service;

import com.devstack.pos.entity.PurchaseOrder;
import com.devstack.pos.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {
    
    private final PurchaseOrderRepository purchaseOrderRepository;
    
    public PurchaseOrder savePurchaseOrder(PurchaseOrder purchaseOrder) {
        return purchaseOrderRepository.save(purchaseOrder);
    }
    
    public PurchaseOrder updatePurchaseOrder(PurchaseOrder purchaseOrder) {
        return purchaseOrderRepository.save(purchaseOrder);
    }
    
    @Transactional(readOnly = true)
    public Optional<PurchaseOrder> findById(Integer id) {
        return purchaseOrderRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Optional<PurchaseOrder> findByPoNumber(String poNumber) {
        return purchaseOrderRepository.findByPoNumber(poNumber);
    }
    
    @Transactional(readOnly = true)
    public List<PurchaseOrder> findBySupplierName(String supplierName) {
        return purchaseOrderRepository.findBySupplierName(supplierName);
    }
    
    @Transactional(readOnly = true)
    public List<PurchaseOrder> findByStatus(String status) {
        return purchaseOrderRepository.findByStatus(status);
    }
    
    @Transactional(readOnly = true)
    public List<PurchaseOrder> findAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<PurchaseOrder> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return purchaseOrderRepository.findByOrderDateBetween(startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public List<PurchaseOrder> findByStatusAndOrderDateBetween(String status, LocalDateTime startDate, LocalDateTime endDate) {
        return purchaseOrderRepository.findByStatusAndOrderDateBetween(status, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public List<PurchaseOrder> searchPurchaseOrders(String poNumber, String supplierName, String status,
                                                     LocalDateTime startDate, LocalDateTime endDate) {
        return purchaseOrderRepository.searchPurchaseOrders(poNumber, supplierName, status, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public Long countByStatus(String status) {
        Long count = purchaseOrderRepository.countByStatus(status);
        return count != null ? count : 0L;
    }
    
    @Transactional(readOnly = true)
    public Double getTotalPurchaseAmount() {
        Double total = purchaseOrderRepository.getTotalPurchaseAmount();
        return total != null ? total : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getTotalPurchaseAmountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double total = purchaseOrderRepository.getTotalPurchaseAmountByDateRange(startDate, endDate);
        return total != null ? total : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getTotalOutstandingAmount() {
        Double total = purchaseOrderRepository.getTotalOutstandingAmount();
        return total != null ? total : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Long countPurchasesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Long count = purchaseOrderRepository.countPurchasesByDateRange(startDate, endDate);
        return count != null ? count : 0L;
    }
    
    public PurchaseOrder approvePurchaseOrder(Integer id, String approvedBy) {
        Optional<PurchaseOrder> optionalPO = purchaseOrderRepository.findById(id);
        if (optionalPO.isPresent()) {
            PurchaseOrder purchaseOrder = optionalPO.get();
            purchaseOrder.setStatus("APPROVED");
            purchaseOrder.setApprovedBy(approvedBy);
            return purchaseOrderRepository.save(purchaseOrder);
        }
        throw new RuntimeException("Purchase order not found with id: " + id);
    }
    
    public PurchaseOrder receivePurchaseOrder(Integer id) {
        Optional<PurchaseOrder> optionalPO = purchaseOrderRepository.findById(id);
        if (optionalPO.isPresent()) {
            PurchaseOrder purchaseOrder = optionalPO.get();
            purchaseOrder.setStatus("RECEIVED");
            purchaseOrder.setReceivedDate(LocalDateTime.now());
            return purchaseOrderRepository.save(purchaseOrder);
        }
        throw new RuntimeException("Purchase order not found with id: " + id);
    }
    
    public PurchaseOrder cancelPurchaseOrder(Integer id) {
        Optional<PurchaseOrder> optionalPO = purchaseOrderRepository.findById(id);
        if (optionalPO.isPresent()) {
            PurchaseOrder purchaseOrder = optionalPO.get();
            purchaseOrder.setStatus("CANCELLED");
            return purchaseOrderRepository.save(purchaseOrder);
        }
        throw new RuntimeException("Purchase order not found with id: " + id);
    }
    
    public PurchaseOrder updatePayment(Integer id, Double paymentAmount) {
        Optional<PurchaseOrder> optionalPO = purchaseOrderRepository.findById(id);
        if (optionalPO.isPresent()) {
            PurchaseOrder purchaseOrder = optionalPO.get();
            Double currentPaidAmount = purchaseOrder.getPaidAmount() != null ? purchaseOrder.getPaidAmount() : 0.0;
            purchaseOrder.setPaidAmount(currentPaidAmount + paymentAmount);
            return purchaseOrderRepository.save(purchaseOrder);
        }
        throw new RuntimeException("Purchase order not found with id: " + id);
    }
    
    public void deletePurchaseOrder(Integer id) {
        purchaseOrderRepository.deleteById(id);
    }
}

