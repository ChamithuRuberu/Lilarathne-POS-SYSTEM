package com.devstack.pos.repository;

import com.devstack.pos.entity.SuperAdminOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuperAdminOrderDetailRepository extends JpaRepository<SuperAdminOrderDetail, Long> {
    // Basic CRUD operations are provided by JpaRepository
    // Add custom queries here if needed in the future
}

