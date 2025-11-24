-- Migration script to create separate tables for Super Admin orders
-- These tables are completely separate from existing order tables

-- Create super_admin_order_detail table
CREATE TABLE IF NOT EXISTS super_admin_order_detail (
    code SERIAL PRIMARY KEY,
    issued_date TIMESTAMP NOT NULL,
    total_cost DECIMAL(10,2) NOT NULL,
    customer_id BIGINT,
    customer_name VARCHAR(100),
    discount DECIMAL(10,2) DEFAULT 0.0,
    operator_email VARCHAR(100),
    payment_method VARCHAR(20) DEFAULT 'CASH',
    payment_status VARCHAR(20) DEFAULT 'PAID',
    order_type VARCHAR(20) DEFAULT 'HARDWARE',
    customer_paid DECIMAL(10,2),
    balance DECIMAL(10,2)
);

-- Create super_admin_order_item table
CREATE TABLE IF NOT EXISTS super_admin_order_item (
    id SERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_code INTEGER,
    product_name VARCHAR(200) NOT NULL,
    batch_code VARCHAR(100),
    batch_number VARCHAR(50),
    quantity DECIMAL(10,2) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    discount_per_unit DECIMAL(10,2),
    total_discount DECIMAL(10,2),
    line_total DECIMAL(10,2) NOT NULL,
    is_general_item BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_super_admin_order_item_order 
        FOREIGN KEY (order_id) REFERENCES super_admin_order_detail(code) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_super_admin_order_detail_issued_date 
    ON super_admin_order_detail(issued_date);
CREATE INDEX IF NOT EXISTS idx_super_admin_order_detail_customer_id 
    ON super_admin_order_detail(customer_id);
CREATE INDEX IF NOT EXISTS idx_super_admin_order_detail_operator_email 
    ON super_admin_order_detail(operator_email);
CREATE INDEX IF NOT EXISTS idx_super_admin_order_detail_payment_status 
    ON super_admin_order_detail(payment_status);
CREATE INDEX IF NOT EXISTS idx_super_admin_order_detail_order_type 
    ON super_admin_order_detail(order_type);

CREATE INDEX IF NOT EXISTS idx_super_admin_order_item_order_id 
    ON super_admin_order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_super_admin_order_item_product_code 
    ON super_admin_order_item(product_code);
CREATE INDEX IF NOT EXISTS idx_super_admin_order_item_batch_code 
    ON super_admin_order_item(batch_code);

-- Add comments to tables for documentation
COMMENT ON TABLE super_admin_order_detail IS 'Separate table for Super Admin orders - completely independent from regular order_detail table';
COMMENT ON TABLE super_admin_order_item IS 'Separate table for Super Admin order items - completely independent from regular order_item table';

COMMENT ON COLUMN super_admin_order_detail.code IS 'Primary key - auto-generated order code';
COMMENT ON COLUMN super_admin_order_detail.issued_date IS 'Date and time when order was created';
COMMENT ON COLUMN super_admin_order_detail.total_cost IS 'Total cost of the order';
COMMENT ON COLUMN super_admin_order_detail.customer_id IS 'Reference to customer (can be null for guest orders)';
COMMENT ON COLUMN super_admin_order_detail.customer_name IS 'Customer name (for guest orders or quick reference)';
COMMENT ON COLUMN super_admin_order_detail.discount IS 'Total discount applied to the order';
COMMENT ON COLUMN super_admin_order_detail.operator_email IS 'Email of the super admin who created the order';
COMMENT ON COLUMN super_admin_order_detail.payment_method IS 'Payment method: CASH, CREDIT, CHEQUE';
COMMENT ON COLUMN super_admin_order_detail.payment_status IS 'Payment status: PAID, PENDING';
COMMENT ON COLUMN super_admin_order_detail.order_type IS 'Order type: HARDWARE, CONSTRUCTION';
COMMENT ON COLUMN super_admin_order_detail.customer_paid IS 'Amount paid by customer';
COMMENT ON COLUMN super_admin_order_detail.balance IS 'Balance amount (customer_paid - total_cost)';

COMMENT ON COLUMN super_admin_order_item.id IS 'Primary key - auto-generated item ID';
COMMENT ON COLUMN super_admin_order_item.order_id IS 'Foreign key to super_admin_order_detail';
COMMENT ON COLUMN super_admin_order_item.product_code IS 'Product code (can be null for general items)';
COMMENT ON COLUMN super_admin_order_item.product_name IS 'Product name/description';
COMMENT ON COLUMN super_admin_order_item.batch_code IS 'Batch code for the product';
COMMENT ON COLUMN super_admin_order_item.batch_number IS 'Batch number for the product';
COMMENT ON COLUMN super_admin_order_item.quantity IS 'Quantity ordered (supports decimals)';
COMMENT ON COLUMN super_admin_order_item.unit_price IS 'Price per unit';
COMMENT ON COLUMN super_admin_order_item.discount_per_unit IS 'Discount per unit';
COMMENT ON COLUMN super_admin_order_item.total_discount IS 'Total discount for this line item';
COMMENT ON COLUMN super_admin_order_item.line_total IS 'Total cost for this line item (quantity * unit_price - discount)';
COMMENT ON COLUMN super_admin_order_item.is_general_item IS 'Flag to identify general items (true) vs regular products (false). General items are not in product_detail table.';
COMMENT ON COLUMN super_admin_order_item.created_at IS 'Timestamp when item was created';

