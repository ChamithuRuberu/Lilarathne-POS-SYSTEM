package com.devstack.pos.service;

import com.devstack.pos.entity.LoyaltyCard;
import com.devstack.pos.repository.LoyaltyCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LoyaltyCardService {
    
    private final LoyaltyCardRepository loyaltyCardRepository;
    
    public LoyaltyCard saveLoyaltyCard(LoyaltyCard loyaltyCard) {
        return loyaltyCardRepository.save(loyaltyCard);
    }
    
    public boolean updateLoyaltyCard(LoyaltyCard loyaltyCard) {
        if (loyaltyCardRepository.existsById(Math.toIntExact(loyaltyCard.getCode()))) {
            loyaltyCardRepository.save(loyaltyCard);
            return true;
        }
        return false;
    }
    
    public boolean deleteLoyaltyCard(Integer code) {
        if (loyaltyCardRepository.existsById(code)) {
            loyaltyCardRepository.deleteById(code);
            return true;
        }
        return false;
    }
    
    public LoyaltyCard findLoyaltyCard(Integer code) {
        return loyaltyCardRepository.findById(code).orElse(null);
    }
    
    public LoyaltyCard findByEmail(String email) {
        return loyaltyCardRepository.findByEmail(email).orElse(null);
    }
    
    public List<LoyaltyCard> findAllLoyaltyCards() {
        return loyaltyCardRepository.findAll();
    }
}

