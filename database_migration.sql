-- Database Migration Script
-- This script updates the database schema to match the new Customer and OrderDetail entities
-- Run this script on your PostgreSQL database before starting the application

-- ==============================================
-- 1. Update Customer Table
-- ==============================================

-- First, check if email column exists and has data
-- If there's existing data, we'll handle it carefully

-- Drop the NOT NULL constraint from email column (if it exists)
ALTER TABLE customer ALTER COLUMN email DROP NOT NULL;

-- Drop the email column entirely (since we don't use it anymore)
-- Comment this line if you want to keep historical email data
ALTER TABLE customer DROP COLUMN IF EXISTS email;

-- Drop the salary column entirely (since we don't use it anymore)
ALTER TABLE customer DROP COLUMN IF EXISTS salary;

-- Make sure the id column is properly set up as auto-increment
-- (It should already be if using SERIAL or IDENTITY)
ALTER TABLE customer ALTER COLUMN id SET DEFAULT nextval('customer_id_seq'::regclass);

-- Create sequence if it doesn't exist
CREATE SEQUENCE IF NOT EXISTS customer_id_seq;

-- ==============================================
-- 2. Update Order_Detail Table
-- ==============================================

-- Drop the customer_email column if it exists
ALTER TABLE order_detail DROP COLUMN IF EXISTS customer_email;

-- Add customer_id column (nullable since old orders might not have it)
ALTER TABLE order_detail ADD COLUMN IF NOT EXISTS customer_id BIGINT;

-- Add customer_name column (nullable, defaults to 'Guest' for old orders)
ALTER TABLE order_detail ADD COLUMN IF NOT EXISTS customer_name VARCHAR(100);

-- Update existing orders to have 'Guest' as customer name if null
UPDATE order_detail SET customer_name = 'Guest' WHERE customer_name IS NULL;

-- ==============================================
-- 3. Optional: Add Foreign Key Constraint
-- ==============================================

-- Uncomment the following line if you want to add a foreign key relationship
-- (This will fail if there are orders with customer_id values that don't exist in customer table)
-- ALTER TABLE order_detail ADD CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE SET NULL;

-- ==============================================
-- 4. Verify Changes
-- ==============================================

-- Show the new customer table structure
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'customer'
ORDER BY ordinal_position;

-- Show the new order_detail table structure
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'order_detail'
ORDER BY ordinal_position;

-- ==============================================
-- NOTES:
-- ==============================================
-- 1. Backup your database before running this script!
-- 2. If you have existing data, you may need to handle data migration
-- 3. Make sure no application instances are running when executing this script
-- 4. After running this script, restart your Spring Boot application

