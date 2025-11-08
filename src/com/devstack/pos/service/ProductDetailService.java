package com.devstack.pos.service;

import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.repository.ProductDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductDetailService {
    
    private final ProductDetailRepository productDetailRepository;
    
    /**
     * Save product detail with business validations
     */
    public ProductDetail saveProductDetail(ProductDetail productDetail) {
        // Validate required fields
        validateProductDetail(productDetail);
        
        // Auto-calculate profit margin (entity will do this via @PrePersist/@PreUpdate)
        // Auto-update batch status (entity will do this via @PrePersist/@PreUpdate)
        
        return productDetailRepository.save(productDetail);
    }
    
    /**
     * Update product detail with validation
     */
    public boolean updateProductDetail(ProductDetail productDetail) {
        validateProductDetail(productDetail);
        
        if (productDetailRepository.existsById(productDetail.getCode())) {
            productDetailRepository.save(productDetail);
            return true;
        }
        return false;
    }
    
    /**
     * Validate product detail business rules
     */
    private void validateProductDetail(ProductDetail productDetail) {
        if (productDetail == null) {
            throw new IllegalArgumentException("Product detail cannot be null");
        }
        
        // Validate quantity
        if (productDetail.getQtyOnHand() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        
        // Validate prices
        if (productDetail.getBuyingPrice() < 0) {
            throw new IllegalArgumentException("Buying price cannot be negative");
        }
        
        if (productDetail.getSellingPrice() < 0) {
            throw new IllegalArgumentException("Selling price cannot be negative");
        }
        
        if (productDetail.getShowPrice() < 0) {
            throw new IllegalArgumentException("Show price cannot be negative");
        }
        
        // Business rule: Selling price should ideally be >= buying price
        // (Warning only, not blocking - controller handles confirmation)
        
        // Validate dates
        if (productDetail.getExpiryDate() != null && productDetail.getManufacturingDate() != null) {
            if (!productDetail.getExpiryDate().isAfter(productDetail.getManufacturingDate())) {
                throw new IllegalArgumentException("Expiry date must be after manufacturing date");
            }
        }
        
        // Validate discount rate
        if (productDetail.getDiscountRate() < 0 || productDetail.getDiscountRate() > 100) {
            throw new IllegalArgumentException("Discount rate must be between 0 and 100");
        }
        
        // Validate low stock threshold
        if (productDetail.getLowStockThreshold() != null && productDetail.getLowStockThreshold() < 0) {
            throw new IllegalArgumentException("Low stock threshold cannot be negative");
        }
    }
    
    /**
     * Delete product detail (soft delete by setting status to OUT_OF_STOCK)
     */
    public boolean deleteProductDetail(String code) {
        if (productDetailRepository.existsById(code)) {
            productDetailRepository.deleteById(code);
            return true;
        }
        return false;
    }
    
    /**
     * Find product detail by code
     */
    public ProductDetail findProductDetail(String code) {
        return productDetailRepository.findByCode(code).orElse(null);
    }
    
    /**
     * Find product detail by code - alternative name
     */
    public ProductDetail findProductDetailByCode(String code) {
        return productDetailRepository.findByCode(code).orElse(null);
    }
    
    /**
     * Find all product details
     */
    public List<ProductDetail> findAllProductDetails() {
        return productDetailRepository.findAll();
    }
    
    /**
     * Find product details by product code
     */
    public List<ProductDetail> findByProductCode(int productCode) {
        return productDetailRepository.findByProductCode(productCode);
    }
    
    /**
     * Find active batches for a product (not expired, has stock)
     */
    public List<ProductDetail> findActiveBatchesByProductCode(int productCode) {
        return productDetailRepository.findByProductCode(productCode).stream()
                .filter(pd -> pd.getQtyOnHand() > 0)
                .filter(pd -> !pd.isExpired())
                .collect(Collectors.toList());
    }
    
    /**
     * Find low stock batches for a product
     */
    public List<ProductDetail> findLowStockBatches(int productCode) {
        return productDetailRepository.findByProductCode(productCode).stream()
                .filter(ProductDetail::isLowStock)
                .collect(Collectors.toList());
    }
    
    /**
     * Find expired batches
     */
    public List<ProductDetail> findExpiredBatches() {
        return productDetailRepository.findAll().stream()
                .filter(ProductDetail::isExpired)
                .collect(Collectors.toList());
    }
    
    /**
     * Find batches expiring soon (within specified days)
     */
    public List<ProductDetail> findBatchesExpiringSoon(int days) {
        return productDetailRepository.findAll().stream()
                .filter(pd -> pd.getExpiryDate() != null)
                .filter(pd -> pd.getDaysUntilExpiry() <= days && pd.getDaysUntilExpiry() >= 0)
                .collect(Collectors.toList());
    }
    
    /**
     * Get total stock quantity for a product (all batches)
     */
    public int getTotalStockForProduct(int productCode) {
        return productDetailRepository.findByProductCode(productCode).stream()
                .filter(pd -> pd.getQtyOnHand() > 0)
                .filter(pd -> !pd.isExpired())
                .mapToInt(ProductDetail::getQtyOnHand)
                .sum();
    }
    
    /**
     * Get total value of stock for a product (based on buying price)
     */
    public double getTotalStockValue(int productCode) {
        return productDetailRepository.findByProductCode(productCode).stream()
                .filter(pd -> pd.getQtyOnHand() > 0)
                .filter(pd -> !pd.isExpired())
                .mapToDouble(pd -> pd.getQtyOnHand() * pd.getBuyingPrice())
                .sum();
    }
    
    /**
     * Get average profit margin for a product
     */
    public double getAverageProfitMargin(int productCode) {
        List<ProductDetail> batches = productDetailRepository.findByProductCode(productCode);
        if (batches.isEmpty()) return 0.0;
        
        return batches.stream()
                .filter(pd -> pd.getQtyOnHand() > 0)
                .filter(pd -> !pd.isExpired())
                .mapToDouble(ProductDetail::getProfitMargin)
                .average()
                .orElse(0.0);
    }
    
    /**
     * Find by code or barcode
     */
    public ProductDetail findByCodeWithProduct(String code) {
        return productDetailRepository.findByCodeOrBarcode(code).orElse(null);
    }
    
    /**
     * Find by barcode
     */
    public ProductDetail findByBarcode(String barcode) {
        return productDetailRepository.findByBarcode(barcode).orElse(null);
    }
    
    /**
     * Reduce stock quantity (for sales)
     */
    public boolean reduceStock(String batchCode, int quantity) {
        Optional<ProductDetail> optionalBatch = productDetailRepository.findByCode(batchCode);
        
        if (optionalBatch.isEmpty()) {
            throw new IllegalArgumentException("Batch not found: " + batchCode);
        }
        
        ProductDetail batch = optionalBatch.get();
        
        if (batch.isExpired()) {
            throw new IllegalStateException("Cannot sell from expired batch: " + batchCode);
        }
        
        if (batch.getQtyOnHand() < quantity) {
            throw new IllegalStateException("Insufficient stock. Available: " + batch.getQtyOnHand() + ", Requested: " + quantity);
        }
        
        batch.setQtyOnHand(batch.getQtyOnHand() - quantity);
        productDetailRepository.save(batch);
        
        return true;
    }
    
    /**
     * Increase stock quantity (for returns/adjustments)
     */
    public boolean increaseStock(String batchCode, int quantity) {
        Optional<ProductDetail> optionalBatch = productDetailRepository.findByCode(batchCode);
        
        if (optionalBatch.isEmpty()) {
            throw new IllegalArgumentException("Batch not found: " + batchCode);
        }
        
        ProductDetail batch = optionalBatch.get();
        batch.setQtyOnHand(batch.getQtyOnHand() + quantity);
        productDetailRepository.save(batch);
        
        return true;
    }
    
    /**
     * Restore stock quantity (for returns) - alias for increaseStock
     */
    public boolean restoreStock(String batchCode, int quantity) {
        return increaseStock(batchCode, quantity);
    }
    
    /**
     * Check if product has sufficient stock across all batches
     */
    public boolean hasSufficientStock(int productCode, int requiredQuantity) {
        int totalStock = getTotalStockForProduct(productCode);
        return totalStock >= requiredQuantity;
    }
    
    /**
     * Find all product details by supplier name
     */
    public List<ProductDetail> findBySupplierName(String supplierName) {
        if (supplierName == null || supplierName.trim().isEmpty()) {
            return List.of();
        }
        return productDetailRepository.findBySupplierName(supplierName.trim());
    }
    
    /**
     * Find product details by supplier name within date range
     */
    public List<ProductDetail> findBySupplierNameAndDateRange(String supplierName, 
                                                               java.time.LocalDateTime startDate, 
                                                               java.time.LocalDateTime endDate) {
        if (supplierName == null || supplierName.trim().isEmpty()) {
            return List.of();
        }
        return productDetailRepository.findBySupplierNameAndDateRange(
            supplierName.trim(), startDate, endDate);
    }
}


