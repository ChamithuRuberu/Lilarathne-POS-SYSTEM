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
        try {
            customerRepository.save(customer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean updateCustomer(Customer customer) {
        if (customer.getId() != null && customerRepository.existsById(customer.getId())) {
            customerRepository.save(customer);
            return true;
        }
        return false;
    }
    
    public boolean deleteCustomer(Long id) {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public Customer findCustomer(Long id) {
        return customerRepository.findById(id).orElse(null);
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
    
    public Customer findByContact(String contact) {
        if (contact == null || contact.trim().isEmpty()) {
            return null;
        }
        List<Customer> customers = customerRepository.searchCustomers(contact.trim());
        return customers.isEmpty() ? null : customers.get(0);
    }
}

