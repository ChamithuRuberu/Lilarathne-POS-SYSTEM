package com.devstack.pos.service;

import com.devstack.pos.entity.Customer;
import com.devstack.pos.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    
    public boolean saveCustomer(Customer customer) {
        if (customerRepository.existsById(customer.getEmail())) {
            return false;
        }
        customerRepository.save(customer);
        return true;
    }
    
    public boolean updateCustomer(Customer customer) {
        if (customerRepository.existsById(customer.getEmail())) {
            customerRepository.save(customer);
            return true;
        }
        return false;
    }
    
    public boolean deleteCustomer(String email) {
        if (customerRepository.existsById(email)) {
            customerRepository.deleteById(email);
            return true;
        }
        return false;
    }
    
    public Customer findCustomer(String email) {
        return customerRepository.findByEmail(email).orElse(null);
    }
    
    public List<Customer> findAllCustomers() {
        return customerRepository.findAll();
    }
    
    public List<Customer> searchCustomers(String search) {
        if (search == null || search.trim().isEmpty()) {
            return findAllCustomers();
        }
        return customerRepository.searchCustomers(search.trim());
    }
}

