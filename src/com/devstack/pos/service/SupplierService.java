package com.devstack.pos.service;

import com.devstack.pos.entity.Supplier;
import com.devstack.pos.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {
    
    private final SupplierRepository supplierRepository;
    
    public boolean saveSupplier(Supplier supplier) {
        try {
            // Check if email already exists (if provided)
            if (supplier.getEmail() != null && !supplier.getEmail().trim().isEmpty()) {
                Optional<Supplier> existingByEmail = supplierRepository.findByEmail(supplier.getEmail().trim());
                if (existingByEmail.isPresent() && !existingByEmail.get().getId().equals(supplier.getId())) {
                    throw new IllegalArgumentException("Supplier with this email already exists!");
                }
            }
            
            // Check if phone already exists (if provided)
            if (supplier.getPhone() != null && !supplier.getPhone().trim().isEmpty()) {
                Optional<Supplier> existingByPhone = supplierRepository.findByPhone(supplier.getPhone().trim());
                if (existingByPhone.isPresent() && !existingByPhone.get().getId().equals(supplier.getId())) {
                    throw new IllegalArgumentException("Supplier with this phone number already exists!");
                }
            }
            
            supplierRepository.save(supplier);
            return true;
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateSupplier(Supplier supplier) {
        if (supplier.getId() != null && supplierRepository.existsById(supplier.getId())) {
            return saveSupplier(supplier); // Reuse save logic which includes validation
        }
        return false;
    }
    
    public boolean deleteSupplier(Long id) {
        if (supplierRepository.existsById(id)) {
            // Soft delete by setting status to INACTIVE
            Optional<Supplier> supplier = supplierRepository.findById(id);
            if (supplier.isPresent()) {
                supplier.get().setStatus("INACTIVE");
                supplierRepository.save(supplier.get());
                return true;
            }
        }
        return false;
    }
    
    public boolean hardDeleteSupplier(Long id) {
        if (supplierRepository.existsById(id)) {
            supplierRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public Supplier findSupplier(Long id) {
        return supplierRepository.findById(id).orElse(null);
    }
    
    public List<Supplier> findAllSuppliers() {
        return supplierRepository.findAll();
    }
    
    public List<Supplier> findActiveSuppliers() {
        return supplierRepository.findByStatus("ACTIVE");
    }
    
    public List<Supplier> searchSuppliers(String search) {
        if (search == null || search.trim().isEmpty()) {
            return findAllSuppliers();
        }
        return supplierRepository.searchSuppliers(search.trim());
    }
    
    public long countSuppliers() {
        return supplierRepository.count();
    }
    
    public long countActiveSuppliers() {
        return supplierRepository.findByStatus("ACTIVE").size();
    }
}

