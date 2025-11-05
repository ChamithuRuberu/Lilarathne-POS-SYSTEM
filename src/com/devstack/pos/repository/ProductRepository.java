package com.devstack.pos.repository;

import com.devstack.pos.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    
    boolean existsByBarcode(String barcode);
    
    Optional<Product> findByBarcode(String barcode);
}

