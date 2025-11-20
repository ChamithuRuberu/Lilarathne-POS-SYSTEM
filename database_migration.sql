-- Migration script to add trial version columns to system_settings table
-- Run this script in your PostgreSQL database before starting the application
-- 
-- To run this script:
-- 1. Open PostgreSQL command line or pgAdmin
-- 2. Connect to your database (robotikka)
-- 3. Run: \i database_migration.sql
--    OR copy and paste the commands below

-- Add trial_enabled column (nullable, will default to false in application)
ALTER TABLE system_settings 
ADD COLUMN IF NOT EXISTS trial_enabled BOOLEAN;

-- Set default value for existing rows
UPDATE system_settings 
SET trial_enabled = false 
WHERE trial_enabled IS NULL;

-- Add trial_end_date column (nullable)
ALTER TABLE system_settings 
ADD COLUMN IF NOT EXISTS trial_end_date DATE;

