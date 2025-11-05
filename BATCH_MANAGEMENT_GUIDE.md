# 📦 Batch Management System - User Guide

## Overview
This guide explains the comprehensive batch management system implemented for your POS application with professional business logic and quality controls.

---

## 🌟 Key Features

### 1. **Comprehensive Batch Tracking**
- ✅ Unique batch numbers (auto-generated or manual)
- ✅ Manufacturing and expiry dates
- ✅ Supplier information tracking
- ✅ Batch status monitoring (ACTIVE, LOW_STOCK, OUT_OF_STOCK, EXPIRED)
- ✅ Low stock threshold alerts
- ✅ Barcode generation for each batch

### 2. **Intelligent Pricing Management**
- ✅ Buying price, selling price, and show price
- ✅ **Auto-calculated profit margin**
- ✅ Color-coded profit indicators (Red <10%, Orange 10-20%, Green >20%)
- ✅ Discount configuration with rate percentage
- ✅ Price validation (prevents negative prices)

### 3. **Quality Business Logic**
- ✅ Quantity tracking (initial quantity + current stock)
- ✅ Stock validation (prevents negative quantities)
- ✅ Date validation (manufacturing < expiry)
- ✅ Expiry warnings (alerts for expired or soon-to-expire batches)
- ✅ Loss prevention (warns if selling price < buying price)
- ✅ Profit margin alerts (warns if margin < 5%)

### 4. **Advanced Validations**
- ✅ Real-time input validation
- ✅ Numeric field restrictions
- ✅ Date range validation
- ✅ Business rule enforcement
- ✅ User confirmation for risky operations

---

## 📋 Batch Form Fields

### Product Information (Read-Only)
| Field | Description |
|-------|-------------|
| **Product Barcode** | The barcode of the parent product |
| **Product Code** | Auto-filled product identifier |
| **Description** | Product description |

### Pricing Details
| Field | Required | Description |
|-------|----------|-------------|
| **Quantity** | ✅ Yes | Initial stock quantity for this batch |
| **Low Stock Threshold** | ⚪ Optional | Alert when stock falls below this number (default: 10) |
| **Buying Price** | ✅ Yes | Cost price per unit |
| **Selling Price** | ✅ Yes | Price to sell per unit |
| **Show Price** | ✅ Yes | Display price for customers |
| **Profit Margin** | Auto | Automatically calculated as: `((Selling - Buying) / Buying) × 100` |

### Discount Configuration
| Field | Required | Description |
|-------|----------|-------------|
| **Discount Available** | ✅ Yes | Yes/No radio button |
| **Discount Rate (%)** | Conditional | Required if discount is available (0-100%) |

### Batch Tracking
| Field | Required | Description |
|-------|----------|-------------|
| **Batch Number** | ⚪ Optional | Auto-generated format: `B{ProductCode}-{Date}-{Random}` |
| **Manufacturing Date** | ⚪ Optional | When the batch was manufactured |
| **Expiry Date** | ⚪ Optional | When the batch expires |

### Supplier Information
| Field | Required | Description |
|-------|----------|-------------|
| **Supplier Name** | ⚪ Optional | Name of the supplier |
| **Supplier Contact** | ⚪ Optional | Phone/Email of supplier |

### Additional Information
| Field | Required | Description |
|-------|----------|-------------|
| **Notes** | ⚪ Optional | Any additional notes about the batch |

---

## ✅ Validation Rules

### 1. **Quantity Validations**
- ✅ Must be greater than 0
- ✅ Cannot exceed 1,000,000 (sanity check)
- ⚠️ Warns if low stock threshold ≥ quantity

### 2. **Price Validations**
- ✅ Cannot be negative
- ⚠️ Confirms if selling price < buying price (loss scenario)
- ⚠️ Warns if profit margin < 5%

### 3. **Date Validations**
- ✅ Manufacturing date cannot be in the future
- ⚠️ Confirms if expiry date is in the past
- ⚠️ Warns if expiry is within 30 days
- ✅ Expiry date must be after manufacturing date

### 4. **Discount Validations**
- ✅ Discount rate must be 0-100%
- ✅ Required if "Discount Available" is selected

---

## 🎨 Visual Feedback

### Profit Margin Color Coding
```
🔴 RED    : < 10%  (Low profit, review pricing)
🟠 ORANGE : 10-20% (Moderate profit)
🟢 GREEN  : > 20%  (Good profit margin)
```

### Date Field Indicators
```
🔴 Red Border : Invalid date (e.g., expiry before manufacturing)
⚪ Normal     : Valid date
```

---

## 🚀 Usage Workflow

### Adding a New Batch

1. **Select Product**: Choose a product from the product list
2. **Click "New Batch"**: Opens the batch creation form
3. **Fill Required Fields**:
   - Quantity
   - Buying Price
   - Selling Price
   - Show Price
4. **Review Profit Margin**: Check auto-calculated profit percentage
5. **Add Optional Information**:
   - Dates (manufacturing/expiry)
   - Supplier details
   - Batch number (or use auto-generated)
   - Low stock threshold
6. **Configure Discount** (if applicable)
7. **Add Notes** (optional)
8. **Save**: System validates and saves the batch

### Editing an Existing Batch

1. **Select Batch**: Click on a batch from the batch list
2. **Click "Edit"**: Opens the batch in edit mode
3. **Modify Fields**: Update quantity, prices, dates, etc.
4. **Save**: Updates the existing batch

### Batch Status Indicators

The system automatically updates batch status:

| Status | Condition | Color |
|--------|-----------|-------|
| **ACTIVE** | Stock available, not expired | 🟢 Green |
| **LOW_STOCK** | Quantity ≤ threshold | 🟡 Yellow |
| **OUT_OF_STOCK** | Quantity = 0 | 🟠 Orange |
| **EXPIRED** | Past expiry date | 🔴 Red |

---

## 🔧 Business Logic Features

### 1. **Stock Management**
```java
// Auto-tracks initial quantity vs current quantity
Initial Qty: 100 units
Current Qty: 75 units
Sold: 25 units
```

### 2. **Profit Calculation**
```java
// Formula
Profit Margin = ((Selling Price - Buying Price) / Buying Price) × 100

// Example
Buying: $10.00
Selling: $15.00
Profit Margin: 50.00% 🟢
```

### 3. **Expiry Tracking**
```java
// System calculates days until expiry
- Expired: Red alert, cannot sell
- Expiring in 1-30 days: Yellow warning
- > 30 days: Normal
```

### 4. **Low Stock Alerts**
```java
// Automatic alerts when stock falls below threshold
Quantity: 8 units
Threshold: 10 units
Status: LOW_STOCK ⚠️
```

---

## 📊 Service Layer Methods

### Stock Queries
```java
// Get total stock for a product (all active batches)
getTotalStockForProduct(int productCode)

// Get total value of stock
getTotalStockValue(int productCode)

// Check if sufficient stock available
hasSufficientStock(int productCode, int requiredQuantity)

// Get average profit margin
getAverageProfitMargin(int productCode)
```

### Batch Filtering
```java
// Find active batches only
findActiveBatchesByProductCode(int productCode)

// Find low stock batches
findLowStockBatches(int productCode)

// Find expired batches
findExpiredBatches()

// Find batches expiring within X days
findBatchesExpiringSoon(int days)
```

### Stock Operations
```java
// Reduce stock (for sales)
reduceStock(String batchCode, int quantity)

// Increase stock (for returns)
increaseStock(String batchCode, int quantity)
```

---

## 🗄️ Database Schema

### New Fields Added to `product_detail` Table

| Column | Type | Description |
|--------|------|-------------|
| `batch_number` | VARCHAR(50) | Human-readable batch number |
| `initial_qty` | INTEGER | Original quantity when batch created |
| `profit_margin` | DECIMAL(10,2) | Auto-calculated profit percentage |
| `discount_rate` | DECIMAL(5,2) | Discount percentage (0-100) |
| `supplier_name` | VARCHAR(200) | Supplier name |
| `supplier_contact` | VARCHAR(100) | Supplier phone/email |
| `manufacturing_date` | DATE | Manufacturing date |
| `expiry_date` | DATE | Expiry date |
| `low_stock_threshold` | INTEGER | Alert threshold |
| `batch_status` | VARCHAR(50) | ACTIVE, LOW_STOCK, OUT_OF_STOCK, EXPIRED |
| `notes` | TEXT | Additional notes |
| `created_at` | TIMESTAMP | Auto-generated creation time |
| `updated_at` | TIMESTAMP | Auto-updated modification time |

### Database Constraints
```sql
-- Ensure selling price >= buying price (at DB level)
CHECK (selling_price >= buying_price)

-- Ensure quantity is not negative
CHECK (qty_on_hand >= 0)

-- Ensure expiry > manufacturing
CHECK (expiry_date IS NULL OR manufacturing_date IS NULL OR expiry_date > manufacturing_date)

-- Ensure valid batch status
CHECK (batch_status IN ('ACTIVE', 'LOW_STOCK', 'OUT_OF_STOCK', 'EXPIRED'))
```

---

## 🎯 Best Practices

### 1. **Always Set Expiry Dates**
For perishable goods, always set manufacturing and expiry dates to enable automatic expiry tracking.

### 2. **Configure Low Stock Thresholds**
Set appropriate thresholds based on:
- Product sales velocity
- Reorder lead time
- Storage capacity

### 3. **Monitor Profit Margins**
- Aim for >20% profit margin (Green zone)
- Review products with <10% margin
- Consider market competition

### 4. **Regular Stock Audits**
Use the service methods to:
- Check expired batches weekly
- Monitor batches expiring within 30 days
- Review low stock alerts daily

### 5. **Supplier Tracking**
Always record supplier information for:
- Quality tracking
- Reordering efficiency
- Issue resolution

---

## ⚠️ Important Notes

1. **Edit Mode**: When editing a batch, the barcode and initial quantity are preserved
2. **Batch Code**: Auto-generated and unique for each batch
3. **Status Updates**: Batch status is automatically updated on every save
4. **Profit Margin**: Recalculated automatically when prices change
5. **Date Validation**: System prevents invalid date combinations

---

## 🔒 Data Integrity

The system ensures data integrity through:

- ✅ Database constraints
- ✅ Service layer validations
- ✅ Controller-level business logic
- ✅ Entity lifecycle hooks (@PrePersist, @PreUpdate)

---

## 📈 Reporting Capabilities

### Available Metrics
- Total stock value by product
- Average profit margins
- Low stock alerts
- Expired batch reports
- Expiring soon warnings
- Supplier performance tracking

---

## 🆘 Troubleshooting

### Issue: "Selling price is less than buying price"
**Cause**: You're entering a selling price lower than the buying price.
**Solution**: Review your prices or click "OK" if this is intentional (e.g., clearance sale).

### Issue: "Expiry date is in the past"
**Cause**: The batch is already expired.
**Solution**: Either update the expiry date or accept that the batch is expired (it will be marked as EXPIRED).

### Issue: "Low stock threshold >= quantity"
**Cause**: Your threshold is set too high.
**Solution**: Reduce the threshold or increase the quantity.

---

## 📞 Support

For issues or questions about the batch management system:
1. Check this guide first
2. Review validation messages
3. Contact system administrator

---

**Version**: 1.0.0  
**Last Updated**: 2025-11-05  
**Author**: POS Development Team

