package com.devstack.pos.repository;

import com.devstack.pos.entity.Category;
import com.devstack.pos.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    Optional<Category> findByName(String name);
    
    List<Category> findByStatus(Status status);
    
    boolean existsByName(String name);
}

