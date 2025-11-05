# Database Migration Instructions

## Problem
The application code has been updated to remove email and salary fields from the Customer entity, but your database still has these columns with NOT NULL constraints.

## Solution
You need to update your database schema to match the new entity structure.

## Steps to Fix

### Option 1: Automatic Schema Update (Quick Fix for Development)

1. **Update your `application.properties`** file:
   ```properties
   # Change this line to update the schema automatically
   spring.jpa.hibernate.ddl-auto=update
   ```

2. **Restart your application**
   - This will automatically update the database schema to match your entities
   - ⚠️ **Warning**: Only use `update` in development! For production, use proper migrations.

### Option 2: Manual Database Migration (Recommended for Production)

1. **Backup your database first!**
   ```bash
   pg_dump -U your_username your_database_name > backup_$(date +%Y%m%d).sql
   ```

2. **Connect to your PostgreSQL database**:
   ```bash
   psql -U your_username -d your_database_name
   ```

3. **Run the migration script**:
   ```bash
   psql -U your_username -d your_database_name -f database_migration.sql
   ```

   OR copy and paste the SQL commands from `database_migration.sql` into your database client.

4. **Verify the changes**:
   ```sql
   -- Check customer table structure
   \d customer
   
   -- Check order_detail table structure
   \d order_detail
   ```

5. **Restart your Spring Boot application**

## Expected Table Structure After Migration

### Customer Table
```
Column   | Type         | Nullable | Default
---------|--------------|----------|------------------
id       | bigint       | NO       | nextval(...)
name     | varchar(100) | NO       | 
contact  | varchar(50)  | YES      | 
```

### Order_Detail Table
```
Column         | Type              | Nullable
---------------|-------------------|----------
code           | bigint            | NO
issued_date    | timestamp         | NO
total_cost     | double precision  | NO
customer_id    | bigint            | YES
customer_name  | varchar(100)      | YES
discount       | double precision  | YES
operator_email | varchar(100)      | YES
```

## Troubleshooting

### If you get "column does not exist" errors:
- Make sure you ran the migration script completely
- Check that your `application.properties` has the correct database URL

### If you get foreign key constraint errors:
- Comment out the foreign key constraint section in the migration script
- Run the script again

### If automatic update doesn't work:
- Change `spring.jpa.hibernate.ddl-auto` to `validate` after initial update
- Or use `create-drop` for complete reset (⚠️ **THIS WILL DELETE ALL DATA!**)

## After Migration

1. Test the customer management features:
   - Create a new customer (only name and contact)
   - Update a customer
   - Delete a customer
   - Search for customers

2. Test order placement:
   - Search customer by contact number
   - Place an order
   - Verify order details show customer name instead of email

3. Check your application logs for any remaining errors

## Important Notes

- ✅ Email field is completely removed
- ✅ Salary field is completely removed  
- ✅ Loyalty cards functionality is removed
- ✅ Customer lookup now uses contact/mobile number
- ✅ Orders store customer name for display purposes
- ✅ Guest checkout is supported (customer_name = "Guest")

## Need Help?

If you encounter issues:
1. Check the application logs for specific error messages
2. Verify database connection settings in `application.properties`
3. Make sure you have backup of your data before making changes

