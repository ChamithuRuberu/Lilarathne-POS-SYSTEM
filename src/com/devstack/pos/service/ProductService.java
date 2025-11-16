package com.devstack.pos.service;

import com.devstack.pos.entity.Product;
import com.devstack.pos.enums.Status;
import com.devstack.pos.repository.ProductRepository;
import com.devstack.pos.util.BarcodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    
    public Product saveProduct(Product product) {
        System.out.println("[PRODUCT SERVICE DEBUG] ========== saveProduct() called ==========");
        System.out.println("[PRODUCT SERVICE DEBUG] Product barcode: '" + (product.getBarcode() != null ? product.getBarcode() : "NULL") + "'");
        System.out.println("[PRODUCT SERVICE DEBUG] Product description: '" + product.getDescription() + "'");
        System.out.println("[PRODUCT SERVICE DEBUG] Product code: " + product.getCode());
        
        if (product.getStatus() == null) {
            product.setStatus(Status.ACTIVE);
        }
        
        // Validate barcode
        if (product.getBarcode() == null || product.getBarcode().trim().isEmpty()) {
            System.out.println("[PRODUCT SERVICE DEBUG] ERROR: Barcode is null or empty!");
            throw new IllegalArgumentException("Barcode is required. Please scan or enter a barcode.");
        }
        
        System.out.println("[PRODUCT SERVICE DEBUG] Barcode validation passed: '" + product.getBarcode() + "'");
        
        if (!BarcodeGenerator.isValidBarcode(product.getBarcode())) {
            System.out.println("[PRODUCT SERVICE DEBUG] ERROR: Invalid barcode format!");
            throw new IllegalArgumentException("Invalid barcode format. Use alphanumeric characters only.");
        }
        
        // Check if barcode already exists (only for new products)
        if (product.getCode() == null && productRepository.existsByBarcode(product.getBarcode())) {
            System.out.println("[PRODUCT SERVICE DEBUG] ERROR: Barcode already exists!");
            throw new IllegalArgumentException("Barcode already exists in the system.");
        }
        
        System.out.println("[PRODUCT SERVICE DEBUG] About to save product to repository...");
        Product savedProduct = productRepository.save(product);
        System.out.println("[PRODUCT SERVICE DEBUG] Product saved to repository!");
        System.out.println("[PRODUCT SERVICE DEBUG] Saved product barcode: '" + (savedProduct.getBarcode() != null ? savedProduct.getBarcode() : "NULL") + "'");
        System.out.println("[PRODUCT SERVICE DEBUG] Saved product code: " + savedProduct.getCode());
        
        return savedProduct;
    }
    
    public boolean updateProduct(Product product) {
        if (productRepository.existsById(Math.toIntExact(product.getCode()))) {
            productRepository.save(product);
            return true;
        }
        return false;
    }
    
    public boolean deleteProduct(Integer code) {
        if (productRepository.existsById(code)) {
            productRepository.deleteById(code);
            return true;
        }
        return false;
    }
    
    public Product findProduct(Integer code) {
        return productRepository.findById(code).orElse(null);
    }
    
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }
    
    public int getLastProductId() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return 0;
        }
        return products.stream()
                .mapToInt(Product::getCode)
                .max()
                .orElse(0);
    }
    
    public Product findProductByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode).orElse(null);
    }
    
    public boolean barcodeExists(String barcode) {
        return productRepository.existsByBarcode(barcode);
    }
}

