-- Migration script to add category and barcode support to products

-- Create category table if not exists
CREATE TABLE IF NOT EXISTS category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT chk_category_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- Add barcode column to product table if not exists
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product' AND column_name = 'barcode'
    ) THEN
        ALTER TABLE product ADD COLUMN barcode VARCHAR(100) UNIQUE;
    END IF;
END $$;

-- Add category_id column to product table if not exists
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product' AND column_name = 'category_id'
    ) THEN
        ALTER TABLE product ADD COLUMN category_id INTEGER;
        ALTER TABLE product ADD CONSTRAINT fk_product_category 
            FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Insert default categories
INSERT INTO category (name, description, status) 
VALUES 
    ('Electronics', 'Electronic devices and accessories', 'ACTIVE'),
    ('Groceries', 'Food and beverages', 'ACTIVE'),
    ('Clothing', 'Apparel and fashion items', 'ACTIVE'),
    ('Home & Garden', 'Home improvement and garden supplies', 'ACTIVE'),
    ('Toys', 'Toys and games', 'ACTIVE'),
    ('Books', 'Books and publications', 'ACTIVE'),
    ('Health & Beauty', 'Health and beauty products', 'ACTIVE'),
    ('Sports', 'Sports equipment and accessories', 'ACTIVE'),
    ('General', 'Miscellaneous items', 'ACTIVE')
ON CONFLICT (name) DO NOTHING;

-- Update existing products to generate barcodes if they don't have one
UPDATE product 
SET barcode = 'PRD' || LPAD(code::text, 5, '0')
WHERE barcode IS NULL;

-- Add index on barcode for faster lookups
CREATE INDEX IF NOT EXISTS idx_product_barcode ON product(barcode);

-- Add index on category_id
CREATE INDEX IF NOT EXISTS idx_product_category ON product(category_id);

-- ============================================================================
-- BATCH MANAGEMENT ENHANCEMENT
-- Add comprehensive batch tracking fields to product_detail table
-- ============================================================================

-- Add batch_number column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product_detail' AND column_name = 'batch_number'
    ) THEN
        ALTER TABLE product_detail ADD COLUMN batch_number VARCHAR(50);
    END IF;
END $$;

-- Add initial_qty column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product_detail' AND column_name = 'initial_qty'
    ) THEN
        ALTER TABLE product_detail ADD COLUMN initial_qty INTEGER DEFAULT 0;
        -- Set initial_qty to current qty_on_hand for existing records
        UPDATE product_detail SET initial_qty = qty_on_hand WHERE initial_qty = 0;
    END IF;
END $$;

-- Add profit_margin column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product_detail' AND column_name = 'profit_margin'
    ) THEN
        ALTER TABLE product_detail ADD COLUMN profit_margin DECIMAL(10,2) DEFAULT 0.0;
    END IF;
END $$;

-- Add discount_rate column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product_detail' AND column_name = 'discount_rate'
    ) THEN
        ALTER TABLE product_detail ADD COLUMN discount_rate DECIMAL(5,2) DEFAULT 0.0;
    END IF;
END $$;

-- Add supplier_name column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product_detail' AND column_name = 'supplier_name'
    ) THEN
        ALTER TABLE product_detail ADD COLUMN supplier_name VARCHAR(200);
    END IF;
END $$;

-- Add supplier_contact column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product_detail' AND column_name = 'supplier_contact'
    ) THEN
        ALTER TABLE product_detail ADD COLUMN supplier_contact VARCHAR(100);
    END IF;
END $$;

-- Add manufacturing_date column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product_detail' AND column_name = 'manufacturing_date'
    ) THEN
        ALTER TABLE product_detail ADD COLUMN manufacturing_date DATE;
    END IF;
END $$;

-- Add expiry_date column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product_detail' AND column_name = 'expiry_date'
    ) THEN
        ALTER TABLE product_detail ADD COLUMN expiry_date DATE;
    END IF;
END $$;

-- Add low_stock_threshold column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product_detail' AND column_name = 'low_stock_threshold'
    ) THEN
        ALTER TABLE product_detail ADD COLUMN low_stock_threshold INTEGER;
    END IF;
END $$;

-- Add batch_status column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product_detail' AND column_name = 'batch_status'
    ) THEN
        ALTER TABLE product_detail ADD COLUMN batch_status VARCHAR(50) DEFAULT 'ACTIVE';
    END IF;
END $$;

-- Add notes column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product_detail' AND column_name = 'notes'
    ) THEN
        ALTER TABLE product_detail ADD COLUMN notes TEXT;
    END IF;
END $$;

-- Add created_at column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product_detail' AND column_name = 'created_at'
    ) THEN
        ALTER TABLE product_detail ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    END IF;
END $$;

-- Add updated_at column
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'product_detail' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE product_detail ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    END IF;
END $$;

-- Update profit margins for existing records
UPDATE product_detail 
SET profit_margin = CASE 
    WHEN buying_price > 0 THEN ((selling_price - buying_price) / buying_price) * 100
    ELSE 0
END
WHERE profit_margin = 0 OR profit_margin IS NULL;

-- Update batch status for existing records
UPDATE product_detail 
SET batch_status = CASE 
    WHEN qty_on_hand <= 0 THEN 'OUT_OF_STOCK'
    WHEN qty_on_hand > 0 AND qty_on_hand <= 10 THEN 'LOW_STOCK'
    ELSE 'ACTIVE'
END
WHERE batch_status IS NULL OR batch_status = '';

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_batch_status ON product_detail(batch_status);
CREATE INDEX IF NOT EXISTS idx_expiry_date ON product_detail(expiry_date);
CREATE INDEX IF NOT EXISTS idx_product_code ON product_detail(product_code);
CREATE INDEX IF NOT EXISTS idx_batch_number ON product_detail(batch_number);

-- Add constraint for batch status values (including DELETED for soft delete)
ALTER TABLE product_detail DROP CONSTRAINT IF EXISTS chk_batch_status;
ALTER TABLE product_detail ADD CONSTRAINT chk_batch_status 
    CHECK (batch_status IN ('ACTIVE', 'LOW_STOCK', 'OUT_OF_STOCK', 'EXPIRED', 'DELETED'));

-- Add constraint to ensure selling price >= buying price
ALTER TABLE product_detail DROP CONSTRAINT IF EXISTS chk_selling_price;
ALTER TABLE product_detail ADD CONSTRAINT chk_selling_price 
    CHECK (selling_price >= buying_price);

-- Add constraint to ensure quantity is not negative
ALTER TABLE product_detail DROP CONSTRAINT IF EXISTS chk_qty_positive;
ALTER TABLE product_detail ADD CONSTRAINT chk_qty_positive 
    CHECK (qty_on_hand >= 0);

-- Add constraint to ensure expiry date is after manufacturing date
ALTER TABLE product_detail DROP CONSTRAINT IF EXISTS chk_dates;
ALTER TABLE product_detail ADD CONSTRAINT chk_dates 
    CHECK (expiry_date IS NULL OR manufacturing_date IS NULL OR expiry_date > manufacturing_date);

