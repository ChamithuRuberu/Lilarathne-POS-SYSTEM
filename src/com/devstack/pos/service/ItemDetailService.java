package com.devstack.pos.service;

import com.devstack.pos.entity.ItemDetail;
import com.devstack.pos.repository.ItemDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}

