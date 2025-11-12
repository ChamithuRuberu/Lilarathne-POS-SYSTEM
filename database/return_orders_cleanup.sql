-- ============================================================================
-- RETURN ORDERS SYSTEM - DATABASE CLEANUP
-- This script fixes the return_order table constraint and removes APPROVED status
-- Run this script to update your database schema
-- ============================================================================

-- ============================================================================
-- 1. Fix return_order table constraint (remove APPROVED status)
-- ============================================================================

-- Drop the old constraint
ALTER TABLE return_order DROP CONSTRAINT IF EXISTS chk_return_status;

-- Add new constraint without APPROVED status
ALTER TABLE return_order ADD CONSTRAINT chk_return_status 
    CHECK (status IN ('PENDING', 'REJECTED', 'COMPLETED'));

-- ============================================================================
-- 2. Update any existing APPROVED records to COMPLETED
-- ============================================================================

-- Update any APPROVED return orders to COMPLETED
-- (This preserves data integrity - APPROVED was meant to be a step before COMPLETED)
UPDATE return_order 
SET status = 'COMPLETED',
    completion_date = COALESCE(completion_date, approval_date, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'APPROVED';

-- ============================================================================
-- 3. Optional: Keep approval_date column for backward compatibility
--    (We'll keep it but it won't be used in new records)
--    If you want to remove it, uncomment the following:
-- ============================================================================

-- ALTER TABLE return_order DROP COLUMN IF EXISTS approval_date;

-- ============================================================================
-- 4. Verify the changes
-- ============================================================================

-- Check constraint
SELECT 
    conname AS constraint_name,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'return_order'::regclass
  AND conname = 'chk_return_status';

-- Check for any remaining APPROVED status (should be 0)
SELECT COUNT(*) as approved_count
FROM return_order
WHERE status = 'APPROVED';

-- Show current status distribution
SELECT status, COUNT(*) as count
FROM return_order
GROUP BY status
ORDER BY status;

-- ============================================================================
-- CLEANUP COMPLETE
-- ============================================================================

COMMENT ON CONSTRAINT chk_return_status ON return_order IS 
    'Validates return order status: PENDING, REJECTED, or COMPLETED (APPROVED removed)';

