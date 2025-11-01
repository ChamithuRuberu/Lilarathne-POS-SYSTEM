package com.devstack.pos.repository;

import com.devstack.pos.entity.ItemDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemDetailRepository extends JpaRepository<ItemDetail, Long> {
    List<ItemDetail> findByOrderCode(Integer orderCode);
    List<ItemDetail> findByDetailCode(String detailCode);
}

