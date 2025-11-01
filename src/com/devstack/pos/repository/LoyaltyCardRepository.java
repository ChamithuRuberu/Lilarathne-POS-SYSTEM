package com.devstack.pos.repository;

import com.devstack.pos.entity.LoyaltyCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyCardRepository extends JpaRepository<LoyaltyCard, Integer> {
    Optional<LoyaltyCard> findByEmail(String email);
}

