package com.devstack.pos.repository;

import com.devstack.pos.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    @Query("SELECT s FROM Supplier s WHERE s.name LIKE %:search% OR s.email LIKE %:search% OR s.phone LIKE %:search% OR s.contactPerson LIKE %:search% ORDER BY s.id DESC")
    List<Supplier> searchSuppliers(@Param("search") String search);
    
    @Query("SELECT s FROM Supplier s ORDER BY s.id DESC")
    List<Supplier> findAll();
    
    Optional<Supplier> findByEmail(String email);
    
    Optional<Supplier> findByPhone(String phone);
    
    @Query("SELECT s FROM Supplier s WHERE s.status = :status ORDER BY s.id DESC")
    List<Supplier> findByStatus(@Param("status") String status);
}

