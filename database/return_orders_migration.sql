-- ============================================================================
-- RETURN ORDERS SYSTEM - DATABASE MIGRATION
-- This script creates tables for tracking returns with product-level details
-- and inventory restoration capabilities
-- ============================================================================

-- Create order_item table to track individual products in each order
CREATE TABLE IF NOT EXISTS order_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_code INTEGER NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    batch_code VARCHAR(100),
    batch_number VARCHAR(50),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price >= 0),
    discount_per_unit DECIMAL(10, 2) DEFAULT 0,
    total_discount DECIMAL(10, 2) DEFAULT 0,
    line_total DECIMAL(10, 2) NOT NULL CHECK (line_total >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key to order_detail table
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) 
        REFERENCES order_detail(code) ON DELETE CASCADE
);

-- Create indexes for order_item table
CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_order_item_product_code ON order_item(product_code);
CREATE INDEX IF NOT EXISTS idx_order_item_batch_code ON order_item(batch_code);
CREATE INDEX IF NOT EXISTS idx_order_item_created_at ON order_item(created_at);

-- Create return_order table (if not exists)
CREATE TABLE IF NOT EXISTS return_order (
    id SERIAL PRIMARY KEY,
    return_id VARCHAR(50) UNIQUE NOT NULL,
    order_id INTEGER NOT NULL,
    customer_email VARCHAR(200) NOT NULL,
    original_amount DECIMAL(10, 2) NOT NULL,
    refund_amount DECIMAL(10, 2) NOT NULL CHECK (refund_amount >= 0),
    return_reason VARCHAR(200) NOT NULL,
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_by VARCHAR(200),
    return_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approval_date TIMESTAMP,
    completion_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_return_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED'))
);

-- Create indexes for return_order table
CREATE INDEX IF NOT EXISTS idx_return_order_return_id ON return_order(return_id);
CREATE INDEX IF NOT EXISTS idx_return_order_order_id ON return_order(order_id);
CREATE INDEX IF NOT EXISTS idx_return_order_customer ON return_order(customer_email);
CREATE INDEX IF NOT EXISTS idx_return_order_status ON return_order(status);
CREATE INDEX IF NOT EXISTS idx_return_order_return_date ON return_order(return_date);

-- Create return_order_item table to track individual products being returned
CREATE TABLE IF NOT EXISTS return_order_item (
    id BIGSERIAL PRIMARY KEY,
    return_order_id INTEGER NOT NULL,
    order_item_id BIGINT NOT NULL,
    product_code INTEGER NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    batch_code VARCHAR(100),
    batch_number VARCHAR(50),
    original_quantity INTEGER NOT NULL CHECK (original_quantity > 0),
    return_quantity INTEGER NOT NULL CHECK (return_quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price >= 0),
    refund_amount DECIMAL(10, 2) NOT NULL CHECK (refund_amount >= 0),
    reason VARCHAR(200),
    inventory_restored BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key to return_order table
    CONSTRAINT fk_return_order_item_return_order FOREIGN KEY (return_order_id) 
        REFERENCES return_order(id) ON DELETE CASCADE,
    
    -- Foreign key to order_item table
    CONSTRAINT fk_return_order_item_order_item FOREIGN KEY (order_item_id) 
        REFERENCES order_item(id) ON DELETE CASCADE,
    
    -- Check that return quantity doesn't exceed original quantity
    CONSTRAINT chk_return_quantity CHECK (return_quantity <= original_quantity)
);

-- Create indexes for return_order_item table
CREATE INDEX IF NOT EXISTS idx_return_order_item_return_order_id ON return_order_item(return_order_id);
CREATE INDEX IF NOT EXISTS idx_return_order_item_order_item_id ON return_order_item(order_item_id);
CREATE INDEX IF NOT EXISTS idx_return_order_item_product_code ON return_order_item(product_code);
CREATE INDEX IF NOT EXISTS idx_return_order_item_batch_code ON return_order_item(batch_code);
CREATE INDEX IF NOT EXISTS idx_return_order_item_inventory_restored ON return_order_item(inventory_restored);
CREATE INDEX IF NOT EXISTS idx_return_order_item_created_at ON return_order_item(created_at);

-- ============================================================================
-- MIGRATION: Populate order_item table from existing orders
-- ============================================================================

-- This section would need to be customized based on your existing order structure
-- If you have a cart history or order history, you would migrate that data here
-- For now, this is a placeholder - new orders will automatically populate this table

COMMENT ON TABLE order_item IS 'Stores individual product items for each order, enabling product-level tracking and returns';
COMMENT ON TABLE return_order IS 'Stores return order information including status, dates, and refund amounts';
COMMENT ON TABLE return_order_item IS 'Stores individual products being returned with quantities and refund calculations';

-- ============================================================================
-- TRIGGERS: Auto-update timestamps
-- ============================================================================

-- Trigger function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to return_order table
DROP TRIGGER IF EXISTS update_return_order_updated_at ON return_order;
CREATE TRIGGER update_return_order_updated_at 
    BEFORE UPDATE ON return_order 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to return_order_item table
DROP TRIGGER IF EXISTS update_return_order_item_updated_at ON return_order_item;
CREATE TRIGGER update_return_order_item_updated_at 
    BEFORE UPDATE ON return_order_item 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- VIEWS: Useful reporting views
-- ============================================================================

-- View: Order items with return information
CREATE OR REPLACE VIEW v_order_items_with_returns AS
SELECT 
    oi.id,
    oi.order_id,
    oi.product_code,
    oi.product_name,
    oi.batch_code,
    oi.batch_number,
    oi.quantity as ordered_quantity,
    oi.unit_price,
    oi.line_total,
    COALESCE(SUM(roi.return_quantity), 0) as total_returned_quantity,
    COALESCE(SUM(roi.refund_amount), 0) as total_refunded_amount,
    oi.quantity - COALESCE(SUM(roi.return_quantity), 0) as net_quantity
FROM order_item oi
LEFT JOIN return_order_item roi ON oi.id = roi.order_item_id
GROUP BY oi.id, oi.order_id, oi.product_code, oi.product_name, 
         oi.batch_code, oi.batch_number, oi.quantity, oi.unit_price, oi.line_total;

-- View: Return orders with item counts and totals
CREATE OR REPLACE VIEW v_return_orders_summary AS
SELECT 
    ro.id,
    ro.return_id,
    ro.order_id,
    ro.customer_email,
    ro.original_amount,
    ro.refund_amount,
    ro.return_reason,
    ro.status,
    ro.return_date,
    ro.processed_by,
    COUNT(roi.id) as item_count,
    SUM(roi.return_quantity) as total_items_returned,
    SUM(roi.refund_amount) as calculated_refund_amount,
    CASE 
        WHEN COUNT(CASE WHEN roi.inventory_restored = FALSE THEN 1 END) = 0 THEN TRUE 
        ELSE FALSE 
    END as all_inventory_restored
FROM return_order ro
LEFT JOIN return_order_item roi ON ro.id = roi.return_order_id
GROUP BY ro.id, ro.return_id, ro.order_id, ro.customer_email, ro.original_amount,
         ro.refund_amount, ro.return_reason, ro.status, ro.return_date, ro.processed_by;

COMMENT ON VIEW v_order_items_with_returns IS 'Shows order items with return statistics';
COMMENT ON VIEW v_return_orders_summary IS 'Shows return orders with item counts and inventory restoration status';

-- ============================================================================
-- SAMPLE DATA (Optional - for testing)
-- ============================================================================

-- Uncomment the following lines to insert sample data for testing

-- INSERT INTO return_order (return_id, order_id, customer_email, original_amount, refund_amount, return_reason, status, processed_by)
-- VALUES 
-- ('RET-TEST-001', 1, 'test@customer.com', 1000.00, 500.00, 'Damaged Product', 'PENDING', 'admin@pos.com');

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================

COMMENT ON SCHEMA public IS 'Return Orders System - Migration completed successfully';

-- Display migration summary
SELECT 
    'Migration Complete' as status,
    COUNT(*) as table_count
FROM information_schema.tables 
WHERE table_schema = 'public' 
  AND table_name IN ('order_item', 'return_order', 'return_order_item');

