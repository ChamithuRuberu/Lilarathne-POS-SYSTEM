package com.devstack.pos.service;

import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for converting pipe values from feet measurements to pipe numbers
 * Used for receipt and PDF generation - conversions are not saved to database
 */
@Service
@RequiredArgsConstructor
public class PipeConversionService {
    
    private final ProductService productService;
    private final ProductDetailService productDetailService;
    
    // Mapping of feet measurements to pipe numbers
    // Key: feet measurement (e.g., "12'", "19 1/2'", "13'")
    // Value: List of pipe numbers that correspond to that measurement
    private static final Map<String, List<String>> FEET_TO_PIPE_MAPPING = new HashMap<>();
    
    // Mapping of product codes (PE, NP, WP, AP) with numbers to pipe numbers
    // Key: "feetMeasurement|productCode" (e.g., "12'|PE 14", "13'|NP 8")
    // Value: pipe number (e.g., "pipe 1", "pipe 2")
    private static final Map<String, String> PRODUCT_CODE_TO_PIPE = new HashMap<>();
    
    // Mapping of product codes to feet divisor for quantity calculation
    // Key: product code (e.g., "PE 14", "NP 8")
    // Value: feet divisor to divide quantity by (12.0, 13.0, or 19.5)
    private static final Map<String, Double> PRODUCT_CODE_TO_FEET_DIVISOR = new HashMap<>();
    
    // Pattern to match feet measurements like "12'", "19 1/2'", "13'"
    private static final Pattern FEET_PATTERN = Pattern.compile("(\\d+\\s*\\d*/?\\d*)\\s*'");
    
    // Pattern to match product codes like "PE 14", "NP 8", "WP 1", "AP 1"
    private static final Pattern PRODUCT_CODE_PATTERN = Pattern.compile("(PE|NP|WP|AP)\\s*(\\d+)");
    
    static {
        initializeMappings();
    }
    
    /**
     * Initialize the mapping of feet measurements to pipe numbers
     */
    private static void initializeMappings() {
        // 12' -> pipe (PE 14, NP 8)
        FEET_TO_PIPE_MAPPING.put("12'", Arrays.asList("PE 14", "NP 8"));
        
        // 19 1/2' -> pipe (PE 13, PE 15, NP 9, NP 10, NP 13, WP 1)
        FEET_TO_PIPE_MAPPING.put("19 1/2'", Arrays.asList("PE 13", "PE 15", "NP 9", "NP 10", "NP 13", "WP 1"));
        FEET_TO_PIPE_MAPPING.put("19.5'", Arrays.asList("PE 13", "PE 15", "NP 9", "NP 10", "NP 13", "WP 1"));
        
        // 13' -> pipe (PE 1-12, NP 1-8, NP 11-12, AP 1)
        List<String> pipe13List = new ArrayList<>();
        // PE 1 to PE 12
        for (int i = 1; i <= 12; i++) {
            pipe13List.add("PE " + i);
        }
        // NP 1 to NP 8
        for (int i = 1; i <= 8; i++) {
            pipe13List.add("NP " + i);
        }
        // NP 11, NP 12
        pipe13List.add("NP 11");
        pipe13List.add("NP 12");
        // AP 1
        pipe13List.add("AP 1");
        FEET_TO_PIPE_MAPPING.put("13'", pipe13List);
        
        // Initialize product code to pipe number mapping
        // Note: Pipe numbers are unique per measurement type
        // Key format: "feetMeasurement|productCode"
        
        // For 12' pipes
        PRODUCT_CODE_TO_PIPE.put("12'|PE 14", "pipe 1");
        PRODUCT_CODE_TO_PIPE.put("12'|NP 8", "pipe 2");
        
        // For 19 1/2' pipes
        PRODUCT_CODE_TO_PIPE.put("19 1/2'|PE 13", "pipe 1");
        PRODUCT_CODE_TO_PIPE.put("19 1/2'|PE 15", "pipe 2");
        PRODUCT_CODE_TO_PIPE.put("19 1/2'|NP 9", "pipe 3");
        PRODUCT_CODE_TO_PIPE.put("19 1/2'|NP 10", "pipe 4");
        PRODUCT_CODE_TO_PIPE.put("19 1/2'|NP 13", "pipe 5");
        PRODUCT_CODE_TO_PIPE.put("19 1/2'|WP 1", "pipe 6");
        // Also support "19.5'" format
        PRODUCT_CODE_TO_PIPE.put("19.5'|PE 13", "pipe 1");
        PRODUCT_CODE_TO_PIPE.put("19.5'|PE 15", "pipe 2");
        PRODUCT_CODE_TO_PIPE.put("19.5'|NP 9", "pipe 3");
        PRODUCT_CODE_TO_PIPE.put("19.5'|NP 10", "pipe 4");
        PRODUCT_CODE_TO_PIPE.put("19.5'|NP 13", "pipe 5");
        PRODUCT_CODE_TO_PIPE.put("19.5'|WP 1", "pipe 6");
        
        // For 13' pipes - assign pipe numbers sequentially starting from 1
        int pipeNumber = 1;
        // PE 1 to PE 12
        for (int i = 1; i <= 12; i++) {
            PRODUCT_CODE_TO_PIPE.put("13'|PE " + i, "pipe " + pipeNumber++);
        }
        // NP 1 to NP 8
        for (int i = 1; i <= 8; i++) {
            PRODUCT_CODE_TO_PIPE.put("13'|NP " + i, "pipe " + pipeNumber++);
        }
        // NP 11, NP 12
        PRODUCT_CODE_TO_PIPE.put("13'|NP 11", "pipe " + pipeNumber++);
        PRODUCT_CODE_TO_PIPE.put("13'|NP 12", "pipe " + pipeNumber++);
        // AP 1
        PRODUCT_CODE_TO_PIPE.put("13'|AP 1", "pipe " + pipeNumber);
        
        // Initialize product code to feet divisor mapping for PDF quantity calculation
        // 12' pipes - divide quantity by 12.0
        PRODUCT_CODE_TO_FEET_DIVISOR.put("PE 14", 12.0);
        PRODUCT_CODE_TO_FEET_DIVISOR.put("NP 8", 12.0);
        
        // 19 1/2' pipes - divide quantity by 19.5
        PRODUCT_CODE_TO_FEET_DIVISOR.put("PE 13", 19.5);
        PRODUCT_CODE_TO_FEET_DIVISOR.put("PE 15", 19.5);
        PRODUCT_CODE_TO_FEET_DIVISOR.put("NP 9", 19.5);
        PRODUCT_CODE_TO_FEET_DIVISOR.put("NP 10", 19.5);
        PRODUCT_CODE_TO_FEET_DIVISOR.put("NP 13", 19.5);
        PRODUCT_CODE_TO_FEET_DIVISOR.put("WP 1", 19.5);
        
        // 13' pipes - divide quantity by 13.0
        for (int i = 1; i <= 12; i++) {
            PRODUCT_CODE_TO_FEET_DIVISOR.put("PE " + i, 13.0);
        }
        // NP 1 to NP 7 (NP 8 is already mapped to 12.0 for 12' pipes, so skip it)
        for (int i = 1; i <= 7; i++) {
            PRODUCT_CODE_TO_FEET_DIVISOR.put("NP " + i, 13.0);
        }
        PRODUCT_CODE_TO_FEET_DIVISOR.put("NP 11", 13.0);
        PRODUCT_CODE_TO_FEET_DIVISOR.put("NP 12", 13.0);
        PRODUCT_CODE_TO_FEET_DIVISOR.put("AP 1", 13.0);
    }
    
    /**
     * Convert product name from feet measurement to pipe number format
     * If the product name contains feet measurements and product codes, convert them to pipe format
     * 
     * @param productName Original product name (e.g., "Pipe 12' PE 14", "NP 8 12'", "12' pipe")
     * @param productCode Integer product code to look up Product description if product name doesn't contain code
     * @param batchCode Batch code to look up ProductDetail.code if available
     * @return Converted product name (e.g., "pipe 1", "pipe 2")
     */
    public String convertProductName(String productName, Integer productCode, String batchCode) {
        if (productName == null || productName.trim().isEmpty()) {
            return productName;
        }
        
        String trimmedName = productName.trim();
        
        // Try to extract feet measurement and product code from product name
        String feetMeasurement = extractFeetMeasurement(trimmedName);
        String productCodeStr = extractProductCode(trimmedName);
        
        // If product code string not found in name, try to get it from ProductDetail.code first (batch code)
        if (productCodeStr == null && batchCode != null && !batchCode.trim().isEmpty()) {
            try {
                ProductDetail productDetail = productDetailService.findProductDetail(batchCode);
                if (productDetail != null && productDetail.getCode() != null) {
                    // Try to extract product code from ProductDetail.code
                    productCodeStr = extractProductCode(productDetail.getCode());
                    // Also try to get feet measurement from code if not found in name
                    if (feetMeasurement == null) {
                        feetMeasurement = extractFeetMeasurement(productDetail.getCode());
                    }
                    System.out.println("PipeConversion: ProductDetail Code=" + batchCode + ", Code=" + productDetail.getCode() + 
                                     ", Extracted Code=" + productCodeStr + ", Feet=" + feetMeasurement);
                }
            } catch (Exception e) {
                System.err.println("PipeConversion: Error looking up ProductDetail " + batchCode + ": " + e.getMessage());
            }
        }
        
        // If still not found, try to get it from Product entity
        if (productCodeStr == null && productCode != null) {
            try {
                Product product = productService.findProduct(productCode);
                if (product != null && product.getDescription() != null) {
                    String productDescription = product.getDescription();
                    // Try to extract product code from product description
                    productCodeStr = extractProductCode(productDescription);
                    // Also try to get feet measurement from description if not found in name
                    if (feetMeasurement == null) {
                        feetMeasurement = extractFeetMeasurement(productDescription);
                    }
                    // Debug: print what we found
                    System.out.println("PipeConversion: Product Code=" + productCode + ", Description=" + productDescription + 
                                     ", Extracted Code=" + productCodeStr + ", Feet=" + feetMeasurement);
                }
            } catch (Exception e) {
                // If product lookup fails, continue with what we have
                System.err.println("PipeConversion: Error looking up product " + productCode + ": " + e.getMessage());
            }
        }
        
        // Debug output
        System.out.println("PipeConversion: productName=" + productName + ", productCode=" + productCode + 
                         ", feetMeasurement=" + feetMeasurement + ", productCodeStr=" + productCodeStr);
        
        // If we found both feet measurement and product code, convert to pipe number
        // Use context-aware key: "feetMeasurement|productCode"
        if (feetMeasurement != null && productCodeStr != null) {
            String key = feetMeasurement + "|" + productCodeStr;
            String pipeNumber = PRODUCT_CODE_TO_PIPE.get(key);
            if (pipeNumber != null) {
                return pipeNumber;
            }
        }
        
        // If we only found product code, try to find it in any measurement context
        // This is a fallback - try each known measurement
        if (productCodeStr != null) {
            for (String measurement : FEET_TO_PIPE_MAPPING.keySet()) {
                String key = measurement + "|" + productCodeStr;
                String pipeNumber = PRODUCT_CODE_TO_PIPE.get(key);
                if (pipeNumber != null) {
                    return pipeNumber;
                }
            }
        }
        
        // If we found feet measurement but no product code, check if it's a known measurement
        // Use the feet measurement to determine the pipe number
        if (feetMeasurement != null && productCodeStr == null && FEET_TO_PIPE_MAPPING.containsKey(feetMeasurement)) {
            // Return "pipe" with the first pipe number for that measurement
            List<String> productCodes = FEET_TO_PIPE_MAPPING.get(feetMeasurement);
            if (!productCodes.isEmpty()) {
                String firstCode = productCodes.get(0);
                String key = feetMeasurement + "|" + firstCode;
                String pipeNumber = PRODUCT_CODE_TO_PIPE.get(key);
                if (pipeNumber != null) {
                    System.out.println("PipeConversion: Using feet measurement fallback - " + feetMeasurement + " -> " + pipeNumber);
                    return pipeNumber;
                }
            }
        }
        
        // If no conversion found, return original name
        System.out.println("PipeConversion: No conversion found, returning original: " + productName);
        return productName;
    }
    
    /**
     * Convert product name from feet measurement to pipe number format (overload without batchCode)
     * 
     * @param productName Original product name
     * @param productCode Integer product code
     * @return Converted product name
     */
    public String convertProductName(String productName, Integer productCode) {
        return convertProductName(productName, productCode, null);
    }
    
    /**
     * Convert product name from feet measurement to pipe number format (overload without productCode and batchCode)
     * 
     * @param productName Original product name
     * @return Converted product name
     */
    public String convertProductName(String productName) {
        return convertProductName(productName, null, null);
    }
    
    /**
     * Extract feet measurement from product name
     * Supports formats like "12'", "19 1/2'", "19.5'", "13'"
     */
    private String extractFeetMeasurement(String productName) {
        Matcher matcher = FEET_PATTERN.matcher(productName);
        if (matcher.find()) {
            String feet = matcher.group(1).trim() + "'";
            // Normalize "19.5'" to "19 1/2'"
            if (feet.equals("19.5'")) {
                return "19 1/2'";
            }
            return feet;
        }
        return null;
    }
    
    /**
     * Extract product code (PE, NP, WP, AP) with number from product name
     */
    private String extractProductCode(String productName) {
        Matcher matcher = PRODUCT_CODE_PATTERN.matcher(productName);
        if (matcher.find()) {
            String prefix = matcher.group(1); // PE, NP, WP, or AP
            String number = matcher.group(2); // The number
            return prefix + " " + number;
        }
        return null;
    }
    
    /**
     * Check if a product name contains pipe-related information that should be converted
     */
    public boolean isPipeProduct(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return false;
        }
        
        // Check if it contains feet measurement or product code
        return extractFeetMeasurement(productName) != null || extractProductCode(productName) != null;
    }
    
    /**
     * Get quantity with "Feet" suffix for pipe products in PDF display
     * Returns the actual quantity value with "Feet" appended if it's a pipe product
     * 
     * @param productName Product name containing pipe code
     * @param originalQuantity Original quantity value
     * @param productCode Integer product code to look up Product description if needed
     * @param batchCode Batch code to look up ProductDetail.code if available
     * @return Quantity with "Feet" suffix (e.g., "12 Feet", "13 Feet", "19.5 Feet") if it's a pipe product, otherwise original quantity as string
     */
    public String convertQuantityToFeet(String productName, Double originalQuantity, Integer productCode, String batchCode) {
        if (productName == null || productName.trim().isEmpty()) {
            return originalQuantity != null ? String.valueOf(originalQuantity) : "0";
        }
        
        if (originalQuantity == null || originalQuantity <= 0) {
            return "0";
        }
        
        String productCodeStr = extractProductCode(productName);
        String feetMeasurement = extractFeetMeasurement(productName);
        
        // If product code string not found in name, try to get it from ProductDetail.code first (batch code)
        if (productCodeStr == null && batchCode != null && !batchCode.trim().isEmpty()) {
            try {
                ProductDetail productDetail = productDetailService.findProductDetail(batchCode);
                if (productDetail != null && productDetail.getCode() != null) {
                    productCodeStr = extractProductCode(productDetail.getCode());
                    if (feetMeasurement == null) {
                        feetMeasurement = extractFeetMeasurement(productDetail.getCode());
                    }
                    System.out.println("PipeConversion Qty: ProductDetail Code=" + batchCode + ", Code=" + productDetail.getCode() + 
                                     ", Extracted Code=" + productCodeStr + ", Feet=" + feetMeasurement);
                }
            } catch (Exception e) {
                System.err.println("PipeConversion Qty: Error looking up ProductDetail " + batchCode + ": " + e.getMessage());
            }
        }
        
        // If still not found, try to get it from Product entity
        if (productCodeStr == null && productCode != null) {
            try {
                Product product = productService.findProduct(productCode);
                if (product != null && product.getDescription() != null) {
                    productCodeStr = extractProductCode(product.getDescription());
                    if (feetMeasurement == null) {
                        feetMeasurement = extractFeetMeasurement(product.getDescription());
                    }
                    System.out.println("PipeConversion Qty: Product Code=" + productCode + ", Description=" + 
                                     product.getDescription() + ", Extracted Code=" + productCodeStr + ", Feet=" + feetMeasurement);
                }
            } catch (Exception e) {
                System.err.println("PipeConversion Qty: Error looking up product " + productCode + ": " + e.getMessage());
            }
        }
        
        // Determine feet divisor based on product code or feet measurement
        double feetDivisor = 0.0;
        
        if (productCodeStr != null && PRODUCT_CODE_TO_FEET_DIVISOR.containsKey(productCodeStr)) {
            // Get the divisor from the mapping
            Double divisor = PRODUCT_CODE_TO_FEET_DIVISOR.get(productCodeStr);
            if (divisor != null) {
                feetDivisor = divisor;
            }
        } else if (feetMeasurement != null) {
            // Check if feet measurement matches known pipe types
            if (feetMeasurement.equals("12'")) {
                feetDivisor = 12.0;
            } else if (feetMeasurement.equals("13'")) {
                feetDivisor = 13.0;
            } else if (feetMeasurement.equals("19 1/2'") || feetMeasurement.equals("19.5'")) {
                feetDivisor = 19.5;
            }
        }
        
        // If it's a pipe product, check if quantity is >= feet divisor
        if (feetDivisor > 0) {
            if (originalQuantity >= feetDivisor) {
                // Divide quantity by feet divisor to get number of pipes
                double pipeCount = originalQuantity / feetDivisor;
                // Format: show as integer if whole number, otherwise show 2 decimal places
                String result;
                if (pipeCount == (int)pipeCount) {
                    result = String.valueOf((int)pipeCount);
                } else {
                    result = String.format("%.2f", pipeCount);
                }
                System.out.println("PipeConversion Qty: " + originalQuantity + " / " + feetDivisor + " = " + result + " pipes");
                return result;
            } else {
                // Quantity is less than feet divisor, show quantity with "Feet" suffix
                String quantityStr;
                if (originalQuantity == originalQuantity.intValue()) {
                    quantityStr = String.valueOf(originalQuantity.intValue());
                } else {
                    quantityStr = String.format("%.2f", originalQuantity);
                }
                // Format: "quantity Feet" (e.g., "6 Feet", "10 Feet")
                String result ="Feet " + quantityStr ;
                System.out.println("PipeConversion Qty: " + originalQuantity + " < " + feetDivisor + ", returning: " + result);
                return result;
            }
        }
        
        // If not a pipe product, return original quantity
        System.out.println("PipeConversion Qty: No conversion, returning original: " + originalQuantity);
        return originalQuantity != null ? String.valueOf(originalQuantity) : "0";
    }
    
    /**
     * Convert quantity to feet measurement (overload without batchCode)
     */
    public String convertQuantityToFeet(String productName, Double originalQuantity, Integer productCode) {
        return convertQuantityToFeet(productName, originalQuantity, productCode, null);
    }
    
    /**
     * Convert quantity to feet measurement (overload without productCode and batchCode)
     */
    public String convertQuantityToFeet(String productName, Double originalQuantity) {
        return convertQuantityToFeet(productName, originalQuantity, null, null);
    }
    
    /**
     * Check if a product should have its quantity converted to feet measurement
     */
    public boolean shouldConvertQuantity(String productName, Integer productCode, String batchCode) {
        if (productName == null || productName.trim().isEmpty()) {
            return false;
        }
        
        String productCodeStr = extractProductCode(productName);
        
        // If product code string not found in name, try to get it from ProductDetail.code first (batch code)
        if (productCodeStr == null && batchCode != null && !batchCode.trim().isEmpty()) {
            try {
                ProductDetail productDetail = productDetailService.findProductDetail(batchCode);
                if (productDetail != null && productDetail.getCode() != null) {
                    productCodeStr = extractProductCode(productDetail.getCode());
                    System.out.println("PipeConversion Qty: ProductDetail Code=" + batchCode + ", Code=" + productDetail.getCode() + 
                                     ", Extracted Code=" + productCodeStr);
                }
            } catch (Exception e) {
                System.err.println("PipeConversion Qty: Error looking up ProductDetail " + batchCode + ": " + e.getMessage());
            }
        }
        
        // If still not found, try to get it from Product entity
        if (productCodeStr == null && productCode != null) {
            try {
                Product product = productService.findProduct(productCode);
                if (product != null && product.getDescription() != null) {
                    productCodeStr = extractProductCode(product.getDescription());
                }
            } catch (Exception e) {
                // If product lookup fails, continue with what we have
            }
        }
        
        return productCodeStr != null && PRODUCT_CODE_TO_FEET_DIVISOR.containsKey(productCodeStr);
    }
    
    /**
     * Check if a product should have its quantity converted (overload without batchCode)
     */
    public boolean shouldConvertQuantity(String productName, Integer productCode) {
        return shouldConvertQuantity(productName, productCode, null);
    }
    
    /**
     * Check if a product should have its quantity converted (overload without productCode and batchCode)
     */
    public boolean shouldConvertQuantity(String productName) {
        return shouldConvertQuantity(productName, null, null);
    }
}

