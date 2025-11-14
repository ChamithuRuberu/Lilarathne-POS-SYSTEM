# Bill Printing & Cash Drawer Implementation

## Overview
This document describes the implementation of automatic bill printing and cash drawer opening functionality when completing orders in the POS system.

## Features Implemented

### 1. **Bill Receipt Generation** (`PDFReportService.java`)
- Added `generateBillReceipt(Long orderId)` method to create professional PDF receipts
- Receipt includes:
  - Company/Store header (Lilarathne POS System)
  - Order number, date, customer name, cashier
  - Payment method
  - Itemized list of products with quantities, prices, and totals
  - Discounts (if applicable)
  - Subtotal, total discount, and grand total
  - Payment status (PAID or PENDING)
  - Thank you message

### 2. **Receipt Printer & Cash Drawer Utility** (`ReceiptPrinter.java`)
New utility class created with the following methods:

- **`openCashDrawer()`**: Sends ESC/POS command (0x1B 0x70 0x00 0x19 0xFA) to the default printer to open a connected cash drawer
- **`openReceiptPDF(String pdfFilePath)`**: Opens the generated PDF receipt with the default PDF viewer
- **`printBillAndOpenDrawer(String receiptPdfPath)`**: Combined method that opens the receipt and triggers the cash drawer
- **`isPrinterAvailable()`**: Checks if a default printer is configured
- **`getDefaultPrinterName()`**: Returns the name of the default printer

### 3. **Updated Order Completion Flow** (`PlaceOrderFormController.java`)

The `btnCompleteOrder()` method now:

1. Creates and saves the order (existing functionality)
2. Saves order items (existing functionality)
3. **NEW**: For CASH/PAID orders only:
   - Generates a PDF receipt using `PDFReportService.generateBillReceipt()`
   - Opens the receipt PDF for viewing/printing
   - Sends command to open the cash drawer
   - Shows success message with receipt file location
4. Reduces stock (existing functionality)
5. Clears the form and cart

### 4. **Dependencies Injected**
- `PDFReportService` - For generating receipt PDFs
- `ReceiptPrinter` - For printing and cash drawer operations

## How It Works

### When Order is Completed with CASH Payment:

```java
// 1. Order is saved to database
OrderDetail savedOrder = orderDetailService.saveOrderDetail(orderDetail);

// 2. Order items are saved
orderItemService.saveAllOrderItems(orderItems);

// 3. Stock is reduced
for (CartTm tm : tms) {
    productDetailService.reduceStock(tm.getCode(), tm.getQty());
}

// 4. Receipt is generated and printed
String receiptPath = pdfReportService.generateBillReceipt(savedOrder.getCode());
receiptPrinter.printBillAndOpenDrawer(receiptPath);
```

### When Order is Completed with CREDIT/CHEQUE Payment:
- No receipt is printed (payment is PENDING)
- Stock is NOT reduced (will be reduced when payment is completed)
- Receipt can be printed later when payment is completed via Pending Payments page

## Technical Details

### ESC/POS Cash Drawer Command
The standard command to open a cash drawer connected to a thermal printer:
- **ESC** (0x1B) - Escape character
- **'p'** (0x70) - Drawer kick command
- **m** (0x00) - Pin 2
- **t1** (0x19) - ON time: 25ms × 2ms = 50ms
- **t2** (0xFA) - OFF time: 250ms × 2ms = 500ms

### Receipt PDF Location and Organization
Receipts are **organized by cashier and customer** in a nested folder structure:

**Path Pattern:**
```
~/POS_Receipts/[CashierName]/[CustomerName]/Receipt_[OrderCode]_[Timestamp].pdf
```

**Example Structure:**
```
~/POS_Receipts/
├── admin/
│   ├── Guest/
│   │   ├── Receipt_12345_1699999999999.pdf
│   │   └── Receipt_12346_1699999999999.pdf
│   ├── John_Doe/
│   │   └── Receipt_12350_1699999999999.pdf
│   └── Jane_Smith/
│       └── Receipt_12351_1699999999999.pdf
├── cashier/
│   ├── Guest/
│   │   ├── Receipt_12347_1699999999999.pdf
│   │   └── Receipt_12348_1699999999999.pdf
│   └── Robert_Johnson/
│       └── Receipt_12349_1699999999999.pdf
└── john_doe/
    └── Guest/
        └── Receipt_12352_1699999999999.pdf
```

**Benefits:**
- **Cashier-wise organization**: Each cashier has their own dedicated folder
- **Customer-wise organization**: Receipts are further organized by customer within each cashier folder
- **Guest orders**: All guest orders are stored in a "Guest" folder under each cashier
- **Easy auditing**: Track receipts by both cashier and customer
- **Simplified management**: Easy to find receipts for specific customers
- **Better organization**: Perfect for multi-user systems with multiple customers

**Name Extraction:**
- **Cashier name**: Extracted from operator email (everything before @)
- **Customer name**: Uses customer name from order, or "Guest" if no customer
- **Sanitization**: Special characters are sanitized for file system compatibility
- **Examples**: 
  - `cashier@pos.com` → `cashier`
  - `John Doe` → `John_Doe`
  - `null/empty` → `Guest`

**Fallback:**
- If directory creation fails, receipts fall back to `~/Downloads/` folder
- No transaction is lost due to folder creation issues

### Printer Requirements
- A default printer must be configured in the operating system
- For cash drawer functionality, the printer should be a POS thermal printer with cash drawer port
- If no printer is available, the system will still generate and open the PDF receipt

## Error Handling
- If receipt generation fails, the order is still completed successfully
- User is notified if receipt printing fails but order was saved
- Cash drawer command failure is logged but doesn't interrupt the order flow
- PDF receipt can always be regenerated from the Orders page

## Benefits
1. **Automatic**: No manual steps required after completing order
2. **Professional**: Clean, formatted receipt with all necessary information
3. **Audit Trail**: All receipts are saved with timestamps
4. **Organized**: Receipts are organized by user/cashier for easy access
5. **Flexible**: Works with or without printer hardware
6. **Hardware Integration**: Opens cash drawer automatically for cash transactions
7. **Multi-User Friendly**: Each cashier has their own dedicated receipt folder

## Future Enhancements
Possible improvements for future versions:
- Direct thermal printer integration (without PDF)
- Email receipt option for customers
- SMS receipt option
- Reprint receipt from Order History
- Custom receipt templates
- Multiple copies option
- Receipt customization (logo, footer text, etc.)
- Date-based sub-folders (e.g., `~/POS_Receipts/cashier/2025-11/`)
- Receipt archiving after X days/months
- Search receipts by date range or order number

## Testing

### Test Scenarios:
1. **Cash Payment**: Complete order with cash → Receipt should open and cash drawer should open
2. **Credit Payment**: Complete order with credit → No receipt (PENDING status)
3. **No Printer**: Complete order without printer configured → PDF should still be generated and opened
4. **Multiple Items**: Order with multiple products and discounts → Receipt should show all items correctly

### Manual Testing Steps:
1. Add items to cart
2. Select "Cash" as payment method
3. Click "Complete Order"
4. Verify:
   - Order is saved in database
   - Stock is reduced
   - PDF receipt opens automatically
   - Cash drawer opens (if printer connected)
   - Success message shows receipt file path

## Files Modified
- `src/com/devstack/pos/service/PDFReportService.java` - Added bill receipt generation
- `src/com/devstack/pos/controller/PlaceOrderFormController.java` - Added receipt printing on order completion

## Files Created
- `src/com/devstack/pos/util/ReceiptPrinter.java` - New utility for printing and cash drawer control

## Dependencies
All required dependencies already exist in `pom.xml`:
- `itext7-core` - For PDF generation
- `javafx-controls` - For printing support
- Java Print Service API (built-in)

