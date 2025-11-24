package com.devstack.pos.repository;

import com.devstack.pos.entity.GeneralItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneralItemRepository extends JpaRepository<GeneralItem, Long> {
    
    List<GeneralItem> findAllByOrderByNameAsc();
}

