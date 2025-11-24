package com.devstack.pos.repository;

import com.devstack.pos.entity.SuperAdminOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuperAdminOrderItemRepository extends JpaRepository<SuperAdminOrderItem, Long> {
    
    /**
     * Find all items for a specific super admin order
     */
    @Query("SELECT o FROM SuperAdminOrderItem o WHERE o.orderId = :orderId ORDER BY o.createdAt ASC")
    List<SuperAdminOrderItem> findByOrderId(@Param("orderId") Long orderId);
}

