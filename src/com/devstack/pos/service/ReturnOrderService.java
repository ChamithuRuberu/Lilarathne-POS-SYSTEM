package com.devstack.pos.service;

import com.devstack.pos.entity.ReturnOrder;
import com.devstack.pos.entity.ReturnOrderItem;
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
    private final ReturnOrderItemService returnOrderItemService;
    private final ProductDetailService productDetailService;
    
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
    public List<ReturnOrder> searchReturnOrders(String searchText, Integer orderId, String status, 
                                                 LocalDateTime startDate, LocalDateTime endDate) {
        return returnOrderRepository.searchReturnOrders(searchText, orderId, status, startDate, endDate);
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
    
    public ReturnOrder completeReturn(Integer id, String processedBy) {
        Optional<ReturnOrder> optionalReturn = returnOrderRepository.findById(id);
        if (optionalReturn.isPresent()) {
            ReturnOrder returnOrder = optionalReturn.get();
            returnOrder.setStatus("COMPLETED");
            returnOrder.setCompletionDate(LocalDateTime.now());
            returnOrder.setProcessedBy(processedBy);
            
            // Save the return order first to ensure status is updated
            ReturnOrder savedReturn = returnOrderRepository.save(returnOrder);
            System.out.println("Return order " + savedReturn.getReturnId() + " marked as COMPLETED and saved to database");
            
            // Restore inventory for all items in this return order
            // This is done after saving to ensure the return order status is persisted
            // even if inventory restoration encounters issues
            try {
                restoreInventoryForReturnOrder(id);
                System.out.println("Inventory restored successfully for return order " + id);
            } catch (Exception e) {
                // Log the error but don't fail the transaction
                // The return order status has already been saved
                System.err.println("Warning: Failed to restore inventory for return order " + id + ": " + e.getMessage());
                e.printStackTrace();
            }
            
            return savedReturn;
        }
        throw new RuntimeException("Return order not found with id: " + id);
    }
    
    /**
     * Restore inventory for all items in a return order
     */
    public void restoreInventoryForReturnOrder(Integer returnOrderId) {
        List<ReturnOrderItem> returnItems = returnOrderItemService.findByReturnOrderId(returnOrderId);
        
        for (ReturnOrderItem item : returnItems) {
            if (!item.getInventoryRestored() && item.getBatchCode() != null) {
                try {
                    // Restore stock to the original batch
                    productDetailService.restoreStock(item.getBatchCode(), item.getReturnQuantity());
                    
                    // Mark as restored
                    item.setInventoryRestored(true);
                    returnOrderItemService.updateReturnOrderItem(item);
                } catch (Exception e) {
                    System.err.println("Failed to restore inventory for item: " + item.getId() + 
                                     ", batch: " + item.getBatchCode() + ", error: " + e.getMessage());
                    // Continue with other items even if one fails
                }
            }
        }
    }
    
    /**
     * Process a return order with item-level details
     * Creates the return order and saves all individual return items
     */
    public ReturnOrder processReturnWithItems(ReturnOrder returnOrder, List<ReturnOrderItem> returnItems) {
        try {
            // Save the return order first
            ReturnOrder savedReturn = saveReturnOrder(returnOrder);
            System.out.println("Return order saved with ID: " + savedReturn.getId() + ", Return ID: " + savedReturn.getReturnId());
            
            // Save all return items
            for (ReturnOrderItem item : returnItems) {
                item.setReturnOrderId(savedReturn.getId());
            }
            List<ReturnOrderItem> savedItems = returnOrderItemService.saveAllReturnOrderItems(returnItems);
            System.out.println("Saved " + savedItems.size() + " return order items");
            
            // If status is COMPLETED, immediately restore inventory
            if ("COMPLETED".equals(savedReturn.getStatus())) {
                try {
                    restoreInventoryForReturnOrder(savedReturn.getId());
                } catch (Exception e) {
                    System.err.println("Warning: Failed to restore inventory during return processing: " + e.getMessage());
                    e.printStackTrace();
                    // Don't fail the transaction - return order is already saved
                }
            }
            
            return savedReturn;
        } catch (Exception e) {
            System.err.println("Error processing return with items: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process return order: " + e.getMessage(), e);
        }
    }
    
    public void deleteReturnOrder(Integer id) {
        returnOrderRepository.deleteById(id);
    }
}

