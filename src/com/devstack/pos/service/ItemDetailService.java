package com.devstack.pos.service;

import com.devstack.pos.entity.ItemDetail;
import com.devstack.pos.repository.ItemDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemDetailService {
    
    private final ItemDetailRepository itemDetailRepository;
    
    public ItemDetail saveItemDetail(ItemDetail itemDetail) {
        return itemDetailRepository.save(itemDetail);
    }
    
    public List<ItemDetail> saveItemDetails(List<ItemDetail> itemDetails) {
        return itemDetailRepository.saveAll(itemDetails);
    }
    
    public boolean updateItemDetail(ItemDetail itemDetail) {
        if (itemDetailRepository.existsById(itemDetail.getCode())) {
            itemDetailRepository.save(itemDetail);
            return true;
        }
        return false;
    }
    
    public boolean deleteItemDetail(Long code) {
        if (itemDetailRepository.existsById(code)) {
            itemDetailRepository.deleteById(code);
            return true;
        }
        return false;
    }
    
    public ItemDetail findItemDetail(Long code) {
        return itemDetailRepository.findById(code).orElse(null);
    }
    
    public List<ItemDetail> findAllItemDetails() {
        return itemDetailRepository.findAll();
    }
    
    public List<ItemDetail> findByOrderCode(Integer orderCode) {
        return itemDetailRepository.findByOrderCode(orderCode);
    }
    
    public List<ItemDetail> findByDetailCode(String detailCode) {
        return itemDetailRepository.findByDetailCode(detailCode);
    }
    
    // Analytics methods
    @Transactional(readOnly = true)
    public List<Object[]> getTopSellingProducts() {
        return itemDetailRepository.getTopSellingProducts();
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getTopSellingProductsByRevenue() {
        return itemDetailRepository.getTopSellingProductsByRevenue();
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getSalesByCategory() {
        return itemDetailRepository.getSalesByCategory();
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getProfitByProduct() {
        return itemDetailRepository.getProfitByProduct();
    }
    
    @Transactional(readOnly = true)
    public Double getTotalProfit() {
        Double profit = itemDetailRepository.getTotalProfit();
        return profit != null ? profit : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getCostOfGoodsSold() {
        Double cost = itemDetailRepository.getCostOfGoodsSold();
        return cost != null ? cost : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getTotalRevenue() {
        Double revenue = itemDetailRepository.getTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }
    
    @Transactional(readOnly = true)
    public List<ItemDetail> findItemsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return itemDetailRepository.findItemsByDateRange(startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public Double getProfitByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double profit = itemDetailRepository.getProfitByDateRange(startDate, endDate);
        return profit != null ? profit : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getCostOfGoodsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double cost = itemDetailRepository.getCostOfGoodsByDateRange(startDate, endDate);
        return cost != null ? cost : 0.0;
    }
}

