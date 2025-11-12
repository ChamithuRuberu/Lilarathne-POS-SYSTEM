-- Payment Methods Migration Script
-- This script adds payment method and payment status support to order_detail table
-- Run this script on your PostgreSQL database before starting the application

-- ==============================================
-- Add Payment Method and Payment Status Columns
-- ==============================================

-- Add payment_method column to order_detail table
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'order_detail' AND column_name = 'payment_method'
    ) THEN
        ALTER TABLE order_detail ADD COLUMN payment_method VARCHAR(20) DEFAULT 'CASH';
    END IF;
END $$;

-- Add payment_status column to order_detail table
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'order_detail' AND column_name = 'payment_status'
    ) THEN
        ALTER TABLE order_detail ADD COLUMN payment_status VARCHAR(20) DEFAULT 'PAID';
    END IF;
END $$;

-- Update existing orders to have CASH and PAID as default values
UPDATE order_detail 
SET payment_method = 'CASH', payment_status = 'PAID' 
WHERE payment_method IS NULL OR payment_status IS NULL;

-- Add constraint for payment_method values
ALTER TABLE order_detail DROP CONSTRAINT IF EXISTS chk_payment_method;
ALTER TABLE order_detail ADD CONSTRAINT chk_payment_method 
    CHECK (payment_method IN ('CASH', 'CREDIT', 'CHEQUE'));

-- Add constraint for payment_status values
ALTER TABLE order_detail DROP CONSTRAINT IF EXISTS chk_payment_status;
ALTER TABLE order_detail ADD CONSTRAINT chk_payment_status 
    CHECK (payment_status IN ('PAID', 'PENDING'));

-- Create index for faster queries on pending payments
CREATE INDEX IF NOT EXISTS idx_payment_status ON order_detail(payment_status);
CREATE INDEX IF NOT EXISTS idx_payment_method ON order_detail(payment_method);

-- ==============================================
-- Verify Changes
-- ==============================================

-- Show the updated order_detail table structure
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'order_detail'
ORDER BY ordinal_position;

-- ==============================================
-- NOTES:
-- ==============================================
-- 1. Backup your database before running this script!
-- 2. All existing orders will be marked as CASH and PAID
-- 3. Make sure no application instances are running when executing this script
-- 4. After running this script, restart your Spring Boot application

