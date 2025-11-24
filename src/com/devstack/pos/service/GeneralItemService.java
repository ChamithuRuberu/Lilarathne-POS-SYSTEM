package com.devstack.pos.service;

import com.devstack.pos.entity.GeneralItem;
import com.devstack.pos.repository.GeneralItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GeneralItemService {
    
    private final GeneralItemRepository generalItemRepository;
    
    public GeneralItem saveGeneralItem(GeneralItem generalItem) {
        return generalItemRepository.save(generalItem);
    }
    
    public boolean updateGeneralItem(GeneralItem generalItem) {
        if (generalItemRepository.existsById(generalItem.getId())) {
            generalItemRepository.save(generalItem);
            return true;
        }
        return false;
    }
    
    public boolean deleteGeneralItem(Long id) {
        if (generalItemRepository.existsById(id)) {
            generalItemRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public GeneralItem findGeneralItem(Long id) {
        return generalItemRepository.findById(id).orElse(null);
    }
    
    public List<GeneralItem> findAllGeneralItems() {
        return generalItemRepository.findAllByOrderByNameAsc();
    }
}

