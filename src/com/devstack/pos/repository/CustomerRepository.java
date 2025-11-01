package com.devstack.pos.repository;

import com.devstack.pos.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByEmail(String email);
    
    @Query("SELECT c FROM Customer c WHERE c.email LIKE %:search% OR c.name LIKE %:search% OR c.contact LIKE %:search%")
    List<Customer> searchCustomers(@Param("search") String search);
}

