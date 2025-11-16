-- Migration script to add customer_paid and balance columns to order_detail table

-- Add customer_paid column to order_detail table if not exists
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'order_detail' AND column_name = 'customer_paid'
    ) THEN
        ALTER TABLE order_detail ADD COLUMN customer_paid DECIMAL(10, 2);
    END IF;
END $$;

-- Add balance column to order_detail table if not exists
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'order_detail' AND column_name = 'balance'
    ) THEN
        ALTER TABLE order_detail ADD COLUMN balance DECIMAL(10, 2);
    END IF;
END $$;

-- Update existing records: set customer_paid = total_cost and balance = 0 for existing orders
UPDATE order_detail 
SET customer_paid = total_cost, balance = 0.00 
WHERE customer_paid IS NULL;

