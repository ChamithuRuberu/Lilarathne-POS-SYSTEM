package com.devstack.pos.repository;

import com.devstack.pos.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    
    @Query("SELECT o FROM OrderDetail o WHERE o.customerName = :email")
    List<OrderDetail> findByCustomerName(String email);
}

