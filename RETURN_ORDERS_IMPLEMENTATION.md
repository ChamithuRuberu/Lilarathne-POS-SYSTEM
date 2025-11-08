# Return Orders System - Implementation Guide

## Overview

A comprehensive return orders management system has been implemented for the POS application. This system enables product-level tracking of returns with automatic inventory restoration, detailed refund calculations, and complete audit trails.

---

## 🎯 Key Features

### 1. **Product-Level Return Tracking**
- Track individual products in orders and returns
- Partial returns supported (return some items from an order)
- Batch/Lot tracking for inventory management
- Detailed refund calculations per product

### 2. **Inventory Restoration**
- Automatic inventory restoration when returns are completed
- Batch-level stock updates
- Prevents double-restoration with inventory_restored flag
- Comprehensive error handling

### 3. **Enhanced User Interface**
- Interactive return dialog with product selection
- Quantity spinners for partial returns
- Real-time refund calculation
- Detailed return order viewing with product breakdowns

### 4. **Complete Audit Trail**
- Return status tracking (PENDING, APPROVED, REJECTED, COMPLETED)
- Timestamps for all status changes
- Processor tracking
- Notes and reason tracking

---

## 📊 Database Schema

### New Tables

#### `order_item`
Stores individual products in each order.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| order_id | BIGINT | Foreign key to order_detail |
| product_code | INTEGER | Product identifier |
| product_name | VARCHAR(200) | Product name |
| batch_code | VARCHAR(100) | Batch/lot code |
| batch_number | VARCHAR(50) | Human-readable batch number |
| quantity | INTEGER | Quantity ordered |
| unit_price | DECIMAL(10,2) | Price per unit |
| discount_per_unit | DECIMAL(10,2) | Discount per unit |
| total_discount | DECIMAL(10,2) | Total discount for line |
| line_total | DECIMAL(10,2) | Total for this line item |
| created_at | TIMESTAMP | Creation timestamp |

#### `return_order`
Stores return order header information.

| Column | Type | Description |
|--------|------|-------------|
| id | SERIAL | Primary key |
| return_id | VARCHAR(50) | Unique return identifier (e.g., RET-123456) |
| order_id | INTEGER | Original order ID |
| customer_email | VARCHAR(200) | Customer identifier |
| original_amount | DECIMAL(10,2) | Original order amount |
| refund_amount | DECIMAL(10,2) | Total refund amount |
| return_reason | VARCHAR(200) | Reason for return |
| notes | TEXT | Additional notes |
| status | VARCHAR(20) | PENDING/APPROVED/REJECTED/COMPLETED |
| processed_by | VARCHAR(200) | User who processed |
| return_date | TIMESTAMP | Return initiation date |
| approval_date | TIMESTAMP | Approval date |
| completion_date | TIMESTAMP | Completion date |

#### `return_order_item`
Stores individual products being returned.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| return_order_id | INTEGER | Foreign key to return_order |
| order_item_id | BIGINT | Foreign key to order_item |
| product_code | INTEGER | Product identifier |
| product_name | VARCHAR(200) | Product name |
| batch_code | VARCHAR(100) | Batch/lot code |
| batch_number | VARCHAR(50) | Batch number |
| original_quantity | INTEGER | Original ordered quantity |
| return_quantity | INTEGER | Quantity being returned |
| unit_price | DECIMAL(10,2) | Price per unit |
| refund_amount | DECIMAL(10,2) | Refund for this item |
| reason | VARCHAR(200) | Item-specific return reason |
| inventory_restored | BOOLEAN | Inventory restoration status |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

---

## 🚀 Setup Instructions

### 1. Run Database Migration

```bash
# Connect to your PostgreSQL database
psql -U your_username -d your_database_name

# Run the migration script
\i database/return_orders_migration.sql
```

The migration script will:
- Create the three new tables
- Add indexes for optimal performance
- Create useful views for reporting
- Set up triggers for timestamp management

### 2. Verify Database Setup

```sql
-- Check if tables were created
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
  AND table_name IN ('order_item', 'return_order', 'return_order_item');

-- Verify indexes
SELECT tablename, indexname 
FROM pg_indexes 
WHERE tablename IN ('order_item', 'return_order', 'return_order_item');
```

### 3. Restart Application

The application will automatically detect the new entities and repositories.

---

## 📖 User Guide

### Processing a Return

#### Step 1: Navigate to Return Orders
- From the dashboard, click "Return Orders" or navigate via the sidebar

#### Step 2: Click "Process New Return"
- Click the "+ Process New Return" button in the Return Orders page

#### Step 3: Load Order
1. Enter the Order ID
2. Click "Load" button
3. System will display:
   - Order details (customer, date, amount, operator)
   - All products in the order

#### Step 4: Select Products to Return
1. Check the checkbox for each product you want to return
2. Adjust the return quantity using the spinner (can be partial)
3. Watch the refund amount update in real-time
4. Use "Select All" / "Deselect All" buttons for convenience

#### Step 5: Provide Return Details
1. Select a return reason from the dropdown
2. Add any additional notes (optional)
3. Review the total refund amount

#### Step 6: Process Return
1. Click "Process Return" button
2. System will:
   - Create the return order
   - Save all return items
   - Generate a unique Return ID
   - Set status to PENDING

---

### Viewing Return Details

#### From Return Orders List
1. Click "View Details" button on any return order
2. See comprehensive information:
   - Order information
   - Customer details
   - Return status and dates
   - **All returned products with quantities and refunds**
   - Inventory restoration status

#### For PENDING Returns
The system shows action buttons:
- **Approve & Restore Inventory**: Approve and immediately restore stock
- **Complete Return**: Mark as completed and restore inventory
- **Reject**: Reject the return (no inventory changes)
- **Close**: Just close the dialog

---

### Inventory Restoration

#### Automatic Process
When a return is **Completed** or **Approved**:
1. System finds all return items
2. For each item with a batch code:
   - Adds the return quantity back to the batch
   - Marks `inventory_restored = true`
   - Updates batch status (e.g., from OUT_OF_STOCK to ACTIVE)
3. If any item fails, others continue (fault tolerance)

#### Manual Check
You can verify inventory restoration:
```sql
-- Check inventory restoration status
SELECT 
    roi.id,
    roi.product_name,
    roi.batch_number,
    roi.return_quantity,
    roi.inventory_restored,
    ro.status
FROM return_order_item roi
JOIN return_order ro ON roi.return_order_id = ro.id
WHERE ro.return_id = 'RET-123456';
```

---

## 🔧 Technical Details

### Entity Relationships

```
OrderDetail (1) ──┬── (N) OrderItem
                  │
ReturnOrder (1) ──┼── (N) ReturnOrderItem ──> (1) OrderItem
                  │
ProductDetail <───┘ (inventory restoration)
```

### Return Status Workflow

```
PENDING ──> APPROVED ──> COMPLETED
   │                        │
   └────> REJECTED          │
                            │
                   [Inventory Restored]
```

### Service Methods

#### ReturnOrderService

```java
// Process return with items
ReturnOrder processReturnWithItems(ReturnOrder returnOrder, List<ReturnOrderItem> returnItems)

// Restore inventory for entire return order
void restoreInventoryForReturnOrder(Integer returnOrderId)

// Status transitions
ReturnOrder approveReturn(Integer id, String approvedBy)
ReturnOrder completeReturn(Integer id)
ReturnOrder rejectReturn(Integer id, String rejectedBy)
```

#### OrderItemService

```java
// Find items in order
List<OrderItem> findByOrderId(Long orderId)

// Analytics
Integer getTotalQuantitySoldByProduct(Integer productCode)
Double getTotalRevenueByProduct(Integer productCode)
```

#### ReturnOrderItemService

```java
// Find items in return
List<ReturnOrderItem> findByReturnOrderId(Integer returnOrderId)

// Analytics
Integer getTotalQuantityReturnedByProduct(Integer productCode)
List<ReturnOrderItem> findUnrestoredItems()
```

---

## 📈 Analytics & Reporting

### Built-in Views

#### `v_order_items_with_returns`
Shows order items with return statistics:
```sql
SELECT * FROM v_order_items_with_returns
WHERE order_id = 123;
```

#### `v_return_orders_summary`
Shows return orders with item counts:
```sql
SELECT * FROM v_return_orders_summary
WHERE status = 'PENDING';
```

### Useful Queries

#### Top Returned Products
```sql
SELECT 
    product_name,
    COUNT(*) as return_count,
    SUM(return_quantity) as total_returned,
    SUM(refund_amount) as total_refunded
FROM return_order_item
GROUP BY product_name
ORDER BY return_count DESC
LIMIT 10;
```

#### Returns by Reason
```sql
SELECT 
    return_reason,
    COUNT(*) as count,
    SUM(refund_amount) as total_refund
FROM return_order
GROUP BY return_reason
ORDER BY count DESC;
```

#### Pending Inventory Restorations
```sql
SELECT 
    roi.*,
    ro.return_id,
    ro.status
FROM return_order_item roi
JOIN return_order ro ON roi.return_order_id = ro.id
WHERE roi.inventory_restored = false
  AND ro.status IN ('APPROVED', 'COMPLETED');
```

---

## 🛡️ Error Handling

### Validation Rules

1. **Return Quantity**: Must be > 0 and ≤ ordered quantity
2. **Batch Code**: Must exist in product_detail table
3. **Order ID**: Must exist and have items
4. **Return Reason**: Must be selected
5. **Inventory Restoration**: Can only be done once per item

### Error Scenarios

| Scenario | Handling |
|----------|----------|
| Order not found | Show error alert to user |
| Batch not found | Log error, continue with other items |
| Insufficient stock for restoration | Stock becomes positive (restoration always works) |
| Duplicate restoration | Prevented by `inventory_restored` flag |
| Missing batch code | Item logged but skipped in restoration |

---

## 🎨 UI Components

### ProcessReturnDialog
- **Width**: 1000px
- **Height**: 750px
- **Features**:
  - Order information section
  - Product selection table with checkboxes
  - Quantity spinners
  - Real-time refund calculation
  - Return reason dropdown
  - Notes textarea

### ReturnOrdersForm
- **Features**:
  - Search and filter functionality
  - Date range filtering
  - Statistics cards (Total Returns, Pending, Total Refund)
  - Return orders table
  - View Details with product breakdown
  - Action buttons for PENDING returns

---

## 🔄 Integration with Existing System

### Order Creation (PlaceOrderFormController)
Now saves `OrderItem` records for each cart item:
- Product details
- Batch information
- Quantities and prices
- Discounts

### Product Management (ProductDetailService)
New methods:
- `restoreStock(String batchCode, int quantity)`
- `findProductDetailByCode(String code)`

### Authorization
Return orders accessible by:
- ADMIN (all operations)
- CASHIER (view and create returns)

---

## 📝 Best Practices

### For Store Managers

1. **Review Returns Promptly**: Process PENDING returns quickly
2. **Check Inventory**: Verify physical inventory matches system
3. **Document Well**: Use the notes field for detailed information
4. **Monitor Trends**: Review return reasons regularly

### For Developers

1. **Transaction Management**: All return operations are transactional
2. **Error Logging**: Check logs for failed inventory restorations
3. **Database Backups**: Backup before running migrations
4. **Performance**: Indexes are in place, but monitor query performance

---

## 🐛 Troubleshooting

### Common Issues

#### Issue: Returns not showing in list
**Solution**: Check date range filter and search text

#### Issue: Inventory not restored
**Solution**: 
1. Check if return status is COMPLETED
2. Verify batch_code exists in product_detail
3. Check application logs for errors
4. Run: `SELECT * FROM return_order_item WHERE inventory_restored = false`

#### Issue: Can't process return
**Solution**:
1. Verify order exists
2. Check if order has order_item records
3. Ensure return reason is selected
4. Validate return quantities

### Debug Queries

```sql
-- Check if order has items
SELECT COUNT(*) FROM order_item WHERE order_id = YOUR_ORDER_ID;

-- Check return status
SELECT * FROM return_order WHERE return_id = 'RET-XXX';

-- Verify inventory restoration
SELECT * FROM return_order_item WHERE return_order_id = YOUR_RETURN_ID;
```

---

## 🔮 Future Enhancements

### Potential Improvements

1. **Return Authorization Numbers**: Generate RMA numbers
2. **Partial Refunds**: Support refunding less than full amount
3. **Restocking Fees**: Deduct fees from refund
4. **Return Shipping**: Track return shipping costs
5. **Exchange Orders**: Support product exchanges
6. **Batch Returns**: Process multiple orders at once
7. **Email Notifications**: Auto-email customers on return status
8. **Refund Methods**: Track refund payment methods
9. **Return Analytics Dashboard**: Visual charts and graphs
10. **Export to CSV**: Export return data for analysis

---

## 📞 Support

For issues or questions:
1. Check this documentation
2. Review error logs in application
3. Check database logs
4. Contact development team

---

## 📄 License & Credits

Implemented for Lilarathne POS System
Date: November 2025
Version: 1.0.0

---

## Summary

The Return Orders System provides:
- ✅ Complete product-level tracking
- ✅ Automatic inventory restoration
- ✅ Comprehensive audit trails
- ✅ User-friendly interface
- ✅ Robust error handling
- ✅ Flexible return processing
- ✅ Real-time calculations
- ✅ Detailed reporting capabilities

All changes are backward compatible and do not affect existing functionality.

