# Bill Printing & Cash Drawer - Quick Start Guide

## What's New? 🎉

When you complete an order with **CASH** payment, the system will now automatically:
1. ✅ Generate a professional PDF receipt
2. 📄 Open the receipt for printing
3. 💵 Send a command to open the cash drawer (if you have a POS printer connected)

## How to Use

### Basic Usage (Most Common):

1. **Add items to cart** as usual
2. **Select "Cash"** as the payment method (default)
3. Click **"Complete Order"** button
4. The system will:
   - Save the order
   - Generate a PDF receipt
   - Open the receipt automatically
   - Open the cash drawer (if printer is connected)
5. A success message will show the receipt file location

### Receipt Location

All receipts are **organized by cashier and customer** in a nested folder structure:
- Path: `~/POS_Receipts/[CashierName]/[CustomerName]/Receipt_[OrderNumber]_[Timestamp].pdf`
- Example: `~/POS_Receipts/cashier/John_Doe/Receipt_12345_1731612345678.pdf`
- Guest orders: `~/POS_Receipts/cashier/Guest/Receipt_12345_1731612345678.pdf`

**Structure:**
```
~/POS_Receipts/
├── admin/
│   ├── Guest/
│   │   ├── Receipt_12345_1731612345678.pdf
│   │   └── Receipt_12346_1731612389012.pdf
│   ├── John_Doe/
│   │   └── Receipt_12350_1731612345678.pdf
│   └── Jane_Smith/
│       └── Receipt_12351_1731612345678.pdf
├── cashier/
│   ├── Guest/
│   │   ├── Receipt_12347_1731612456789.pdf
│   │   └── Receipt_12348_1731612567890.pdf
│   └── Robert_Johnson/
│       └── Receipt_12349_1731612678901.pdf
└── john_doe/
    └── Guest/
        └── Receipt_12352_1731612678901.pdf
```

**Organization Benefits:**
- ✅ **By Cashier**: Each cashier has their own folder
- ✅ **By Customer**: Receipts are further organized by customer name
- ✅ **Guest Orders**: All guest orders are stored in a "Guest" folder
- ✅ **Easy to Find**: Quickly locate receipts for specific customers
- ✅ **Better Auditing**: Track receipts by both cashier and customer

## Payment Method Behavior

| Payment Method | Receipt Printed? | Cash Drawer Opens? | Stock Reduced? |
|---------------|------------------|-------------------|----------------|
| Cash          | ✅ Yes           | ✅ Yes            | ✅ Immediately |
| Credit        | ❌ No            | ❌ No             | ⏳ When paid   |
| Cheque        | ❌ No            | ❌ No             | ⏳ When paid   |

**Note**: For Credit/Cheque orders, receipt can be printed later when payment is completed via the Pending Payments page.

## Receipt Contents

The generated receipt includes:
- 🏪 Store name and contact information
- 🧾 Receipt number and date/time
- 👤 Customer name
- 💼 Cashier/operator name
- 🛒 Itemized list of products
  - Product name
  - Quantity
  - Unit price
  - Line total
- 💰 Discounts (if any)
- 📊 Subtotal and grand total
- 💳 Payment method
- ✓ Payment status (PAID/PENDING)
- 👋 Thank you message

## Cash Drawer Setup

### For Cash Drawer to Work:

1. **Connect a POS thermal printer** with cash drawer port
2. **Connect the cash drawer** to the printer's RJ11/RJ12 port
3. **Set the printer as default** in your operating system:
   - **macOS**: System Preferences → Printers & Scanners → Set as default
   - **Windows**: Settings → Devices → Printers → Set as default
   - **Linux**: System Settings → Printers → Set as default

### Without a Cash Drawer:

Don't worry! The system works fine without a cash drawer:
- The receipt will still be generated and opened
- No errors will be shown
- Everything else works normally

## Troubleshooting

### Receipt doesn't open automatically
- **Check**: PDF should still be saved in `~/POS_Receipts/[cashier]/[customer]/` folder
- **Solution**: Open it manually from the POS_Receipts folder
- **Note**: Your OS might be blocking automatic file opens
- **Guest orders**: Look in the `Guest` folder under each cashier

### Cash drawer doesn't open
- **Check**: Is the printer set as default?
- **Check**: Is the cash drawer properly connected to the printer?
- **Check**: Does your printer support ESC/POS commands?
- **Try**: Test the drawer with printer's test button

### "No default printer found" message
- **Solution**: Set up a default printer in your OS settings
- **Note**: The receipt will still be generated, you just need to open it manually

### Receipt generation failed
- **Check**: Do you have write permissions to create folders in your home directory?
- **Check**: Is there enough disk space?
- **Note**: The order will still be saved successfully
- **Fallback**: If POS_Receipts folder can't be created, receipts will be saved to Downloads folder

## Example Receipt

```
========================================
    LILARATHNE POS SYSTEM
    123 Main Street, Colombo
    Tel: +94 11 234 5678
========================================

        SALES RECEIPT

Receipt No: 12345
Date: 14/11/2025 15:30:45
Customer: John Doe
Cashier: cashier@pos.com
Payment Method: CASH

----------------------------------------

Item              Qty   Price    Total
Coca Cola 330ml   2     150.00   300.00
Bread - Large     1     120.00   120.00
   Discount: -10.00

----------------------------------------

Subtotal: LKR 430.00
Total Discount: -LKR 10.00

========================================
TOTAL: LKR 420.00
========================================

*** PAID ***

    Thank you for your business!
       Please come again!

========================================
```

## Tips & Best Practices

1. **Test First**: Do a test transaction to ensure everything works
2. **Keep Receipts**: All receipts are saved and organized by cashier and customer in `~/POS_Receipts/`
3. **Check Printer**: Make sure your printer has paper before busy periods
4. **Backup Receipts**: Consider backing up the `~/POS_Receipts/` folder regularly
5. **Easy Access**: Each cashier can easily find receipts by customer in their dedicated folders
6. **Guest Orders**: Guest orders are stored in the "Guest" folder under each cashier
7. **Customize**: You can update store name/address in `PDFReportService.java`

## Customization

To customize the receipt header (store name, address, phone):

1. Open: `src/com/devstack/pos/service/PDFReportService.java`
2. Find the `generateBillReceipt()` method
3. Update these lines:
   ```java
   Paragraph storeName = new Paragraph("YOUR STORE NAME")
   ...
   Paragraph storeAddress = new Paragraph("Your Address\nTel: Your Phone")
   ```
4. Save and restart the application

## Support

If you encounter any issues:
1. Check this guide first
2. Check the console/logs for error messages
3. Verify printer configuration
4. Ensure all dependencies are properly installed

---

**Enjoy the new automated bill printing feature!** 🎉

