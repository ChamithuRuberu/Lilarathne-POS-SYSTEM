# Product Management Setup Instructions

## 🚀 Quick Start Guide

### Step 1: Database Migration
Before using the updated product module, run the database migration:

```bash
# Connect to your PostgreSQL database
psql -U your_username -d your_database_name

# Or if using a specific host
psql -h localhost -U your_username -d your_database_name

# Then run the migration script
\i database/migration.sql

# Or in one command:
psql -U your_username -d your_database_name -f database/migration.sql
```

This will:
- ✅ Create the `category` table
- ✅ Add `barcode` column to `product` table
- ✅ Add `category_id` foreign key to `product` table
- ✅ Insert 9 default categories
- ✅ Generate barcodes for existing products
- ✅ Create performance indexes

### Step 2: Verify Setup
After running the migration, verify:

```sql
-- Check categories table
SELECT * FROM category;

-- Should show 9 categories:
-- Electronics, Groceries, Clothing, Home & Garden, Toys, Books, Health & Beauty, Sports, General

-- Check product table structure
\d product

-- Should show columns: code, description, barcode, category_id, status
```

### Step 3: Start Application
Run your application:

```bash
# If using Maven
mvn spring-boot:run

# Or if running from IDE
# Run PosApplication.java main method
```

### Step 4: Test Category Management
1. Open the application
2. Navigate to **Product Management**
3. Click **"+ Manage"** button next to Category dropdown
4. Category Management window opens
5. Add a test category:
   - Name: "Test Category"
   - Description: "Testing"
   - Click "Save Category"
6. Verify it appears in the list
7. Close the window
8. The category should now appear in the dropdown

### Step 5: Test Product Creation
1. Click **"+ New Product"** button
2. Enter or scan a barcode (or click "Preview Barcode" to generate)
3. Select a category from dropdown
4. Enter product description
5. Click **"Save Product"**
6. Product should appear in the table below with:
   - Barcode
   - Description
   - Category
   - Blue "View" button
   - Red "Delete" button

---

## 🎯 Features Guide

### Category Management
**Access**: Click "+ Manage" button next to Category field

**Functions**:
- ✅ Add new categories
- ✅ Edit existing categories (click row to load)
- ✅ Delete categories (red button)
- ✅ View all categories with status

### Product Management

#### Adding Products
**Method 1: Barcode Scanner**
1. Focus on barcode field
2. Scan product
3. Press Enter (scanner does this automatically)
4. If product exists, option to load it
5. If new, continue with category and description

**Method 2: Manual Entry**
1. Type barcode manually
2. Or click "Preview Barcode" to auto-generate
3. Select category
4. Enter description
5. Save

#### Managing Products
- **View Batches**: Click blue "View" button
- **Delete Product**: Click red "Delete" button
- **Update Product**: Click "View", modify fields, save

---

## 🔧 Troubleshooting

### Issue: "No categories found"
**Solution**: 
1. Run migration script (Step 1 above)
2. If already run, check database:
   ```sql
   SELECT * FROM category WHERE status = 'ACTIVE';
   ```
3. If empty, manually insert:
   ```sql
   INSERT INTO category (name, description, status) 
   VALUES ('General', 'General products', 'ACTIVE');
   ```

### Issue: "Product list is empty"
**Check**:
1. Database connection is working
2. Products exist in database:
   ```sql
   SELECT * FROM product;
   ```
3. Check console for errors (System.out messages)
4. Verify product table has required columns

### Issue: "Barcode already exists"
**Solution**:
- Click "Preview Barcode" to generate a unique one
- Or use a different barcode value
- Check existing products to avoid duplicates

### Issue: "Buttons not showing in product table"
**Already Fixed**: Buttons now have inline styling
If still not showing, check:
- JavaFX version compatibility
- JFoenix library loaded
- Table columns properly configured

### Issue: "Category dropdown is empty"
**Solutions**:
1. Click "+ Manage" to add categories
2. Check if categories table is populated
3. Restart application to reload categories
4. Check console for "Loading categories" message

---

## 📊 Database Schema

### Category Table
```sql
CREATE TABLE category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);
```

### Product Table (Updated)
```sql
ALTER TABLE product 
ADD COLUMN barcode VARCHAR(100) UNIQUE,
ADD COLUMN category_id INTEGER,
ADD CONSTRAINT fk_product_category 
    FOREIGN KEY (category_id) REFERENCES category(id);
```

---

## 🎨 UI Changes

### Before
- Two input fields (Product Code + Barcode)
- No category support
- Buttons not rendering properly
- Simple table layout

### After
- ✅ Single barcode field (primary identifier)
- ✅ Category dropdown with management
- ✅ Styled, working buttons
- ✅ Enhanced table with barcode and category columns
- ✅ Purple "+ Manage" button for categories
- ✅ Barcode preview functionality

---

## 💡 Usage Tips

1. **First Time Setup**
   - Run migration first
   - Add custom categories as needed
   - Default categories are provided

2. **Barcode Scanner Setup**
   - Configure scanner to send "Enter" after scan
   - Test with barcode field focused
   - Scanner acts like keyboard input

3. **Category Organization**
   - Use meaningful category names
   - Add descriptions for clarity
   - Delete unused categories

4. **Product Entry Workflow**
   - Have categories ready first
   - Use scanner for speed
   - Review barcode preview before saving

---

## 📝 Default Categories

After migration, these categories are available:

1. **Electronics** - Electronic devices and accessories
2. **Groceries** - Food and beverages
3. **Clothing** - Apparel and fashion items
4. **Home & Garden** - Home improvement and garden supplies
5. **Toys** - Toys and games
6. **Books** - Books and publications
7. **Health & Beauty** - Health and beauty products
8. **Sports** - Sports equipment and accessories
9. **General** - Miscellaneous items

---

## 🔄 Migration Rollback (If Needed)

If you need to undo the changes:

```sql
-- Remove foreign key
ALTER TABLE product DROP CONSTRAINT IF EXISTS fk_product_category;

-- Remove columns
ALTER TABLE product DROP COLUMN IF EXISTS category_id;
ALTER TABLE product DROP COLUMN IF EXISTS barcode;

-- Drop indexes
DROP INDEX IF EXISTS idx_product_barcode;
DROP INDEX IF EXISTS idx_product_category;

-- Drop category table
DROP TABLE IF EXISTS category;
```

**⚠️ Warning**: This will delete all category data!

---

## 📞 Support

If you encounter issues:
1. Check console output for error messages
2. Verify database connection
3. Ensure migration ran successfully
4. Check that all dependencies are loaded
5. Review the logs in the application console

---

**Version**: 2.0  
**Last Updated**: November 5, 2025  
**Status**: ✅ Production Ready

