# Customer-Order Relationship Documentation

## Overview
This document explains how customers are connected to their orders in the POS system.

## Database Structure

### Customer Table
```
customer
├── id (Primary Key, Auto-generated)
├── name
└── contact
```

### Order_Detail Table
```
order_detail
├── code (Primary Key, Auto-generated)
├── issued_date
├── total_cost
├── customer_id (Foreign Key → customer.id)
├── customer_name (Denormalized for display)
├── discount
└── operator_email
```

## How the Connection Works

### 1. **Customer ID Link**
- When an order is placed, the `customer_id` field in `order_detail` stores the customer's ID
- This creates a direct link between the customer and their orders
- If no customer is selected (guest checkout), `customer_id` is NULL

### 2. **Customer Name Denormalization**
- We also store `customer_name` directly in orders for quick display
- This avoids JOIN queries for simple order listings
- Guest orders show "Guest" as the customer name

## Code Implementation

### 1. **OrderDetail Entity** (`OrderDetail.java`)
```java
@Column(name = "customer_id")
private Long customerId;

@Column(name = "customer_name", length = 100)
private String customerName;
```
- Uses `customerId` to link to Customer table
- No `@ManyToOne` relationship to avoid lazy loading issues
- Supports NULL values for guest orders

### 2. **Repository Query** (`OrderDetailRepository.java`)
```java
// Get all orders for a specific customer
@Query("SELECT o FROM OrderDetail o WHERE o.customerId = :customerId ORDER BY o.issuedDate DESC")
List<OrderDetail> findByCustomerId(@Param("customerId") Long customerId);

// Get total amount spent by a customer (optimized)
@Query("SELECT COALESCE(SUM(o.totalCost), 0.0) FROM OrderDetail o WHERE o.customerId = :customerId")
Double getTotalSpentByCustomerId(@Param("customerId") Long customerId);
```

### 3. **Service Layer** (`OrderDetailService.java`)
```java
public List<OrderDetail> findByCustomerId(Long customerId) {
    return orderDetailRepository.findByCustomerId(customerId);
}

public Double getTotalSpentByCustomerId(Long customerId) {
    if (customerId == null) {
        return 0.0;
    }
    Double total = orderDetailRepository.getTotalSpentByCustomerId(customerId);
    return total != null ? total : 0.0;
}
```

### 4. **Creating Orders** (`PlaceOrderFormController.java`)
```java
OrderDetail orderDetail = new OrderDetail();
orderDetail.setCustomerId(selectedCustomerId);  // Links to customer
orderDetail.setCustomerName(txtName.getText());  // Stores name for display
orderDetail.setTotalCost(totalAmount);
// ... other fields
```

### 5. **Displaying Customer Totals** (`CustomerFormController.java`)
```java
// For each customer, calculate their total spending
double totalSpent = orderDetailService.getTotalSpentByCustomerId(customer.getId());

CustomerTm tm = new CustomerTm(
    customer.getId(), 
    customer.getName(), 
    customer.getContact(), 
    totalSpent,  // Shows total amount spent
    deleteButton
);
```

## Data Flow

### When Placing an Order:
1. **Customer Search**: Enter contact number → finds customer by contact
2. **Customer Selection**: Sets `selectedCustomerId` from found customer
3. **Order Creation**: 
   - `customerId` = selected customer's ID (or NULL for guest)
   - `customerName` = customer's name (or "Guest")
4. **Order Saved**: Links order to customer in database

### When Viewing Customers:
1. **Load Customers**: Fetch all customers from database
2. **Calculate Spending**: For each customer:
   ```sql
   SELECT SUM(total_cost) 
   FROM order_detail 
   WHERE customer_id = ?
   ```
3. **Display**: Show customer with their total spending

## Query Examples

### Get All Orders for a Customer
```java
List<OrderDetail> orders = orderDetailService.findByCustomerId(customerId);
// Returns all orders placed by this customer, newest first
```

### Get Total Amount Spent by Customer
```java
Double totalSpent = orderDetailService.getTotalSpentByCustomerId(customerId);
// Returns: Rs. 15,450.00 (sum of all their orders)
```

### Get Orders by Customer Name
```java
List<OrderDetail> orders = orderDetailService.findByCustomerName("John");
// Returns all orders where customer name contains "John"
```

## Benefits of This Design

### ✅ **Advantages:**
1. **Performance**: Optimized SUM query for total spending
2. **Flexibility**: Supports guest orders (NULL customer_id)
3. **Fast Display**: Customer name stored directly in orders
4. **Simple**: No complex JOIN queries needed for most operations
5. **Analytics Ready**: Easy to aggregate customer spending

### ⚠️ **Trade-offs:**
1. **Denormalization**: Customer name duplicated in orders
2. **Update Complexity**: If customer name changes, old orders keep old name
3. **Data Consistency**: Must ensure customerId matches customerName

## Usage in Customer Management

The customer table now shows:
```
+----+---------------+-------------+---------------+--------+
| ID | Customer Name | Contact     | Total Spent   | Action |
+----+---------------+-------------+---------------+--------+
| 1  | John Doe      | 0771234567  | Rs. 5,420.00  | Delete |
| 2  | Jane Smith    | 0769876543  | Rs. 12,890.00 | Delete |
| 3  | Bob Wilson    | 0754567890  | Rs. 0.00      | Delete |
+----+---------------+-------------+---------------+--------+
```

- **Total Spent** is calculated in real-time from all orders
- Updates automatically when new orders are placed
- Shows Rs. 0.00 for customers with no orders

## Testing the Connection

### Test 1: Place Order with Customer
1. Go to Place Order page
2. Enter customer contact number
3. Search → customer found and selected
4. Add products and complete order
5. ✅ Order saved with customer_id

### Test 2: View Customer Spending
1. Go to Customer Management
2. See "Total Spent" column
3. ✅ Shows sum of all customer's orders

### Test 3: Guest Order
1. Place order without selecting customer
2. Complete order
3. ✅ Order saved with customer_id = NULL, customer_name = "Guest"

## Related Files

- `src/com/devstack/pos/entity/OrderDetail.java` - Order entity
- `src/com/devstack/pos/entity/Customer.java` - Customer entity
- `src/com/devstack/pos/repository/OrderDetailRepository.java` - Order queries
- `src/com/devstack/pos/service/OrderDetailService.java` - Order business logic
- `src/com/devstack/pos/controller/CustomerFormController.java` - Customer management
- `src/com/devstack/pos/controller/PlaceOrderFormController.java` - Order placement

## Database Migration Note

After updating the code, make sure your database has:
```sql
-- Check order_detail table structure
ALTER TABLE order_detail ADD COLUMN IF NOT EXISTS customer_id BIGINT;
ALTER TABLE order_detail ADD COLUMN IF NOT EXISTS customer_name VARCHAR(100);
```

See `database_migration.sql` for complete migration script.

