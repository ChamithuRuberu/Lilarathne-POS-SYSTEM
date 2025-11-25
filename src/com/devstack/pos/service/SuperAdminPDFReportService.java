package com.devstack.pos.service;

import com.devstack.pos.entity.SuperAdminOrderDetail;
import com.devstack.pos.entity.SuperAdminOrderItem;
import com.devstack.pos.entity.SystemSettings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuperAdminPDFReportService {
    
    private final SuperAdminOrderDetailService superAdminOrderDetailService;
    private final SuperAdminOrderItemService superAdminOrderItemService;
    private final SystemSettingsService systemSettingsService;
    
    private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    /**
     * Generate plain text bill receipt for thermal printer for super admin orders
     */
    public String generateSuperAdminPlainTextReceipt(Long orderId) {
        try {
            // Get super admin order details
            SuperAdminOrderDetail orderDetail = superAdminOrderDetailService.findSuperAdminOrderDetail(orderId);
            if (orderDetail == null) {
                return "Error: Super Admin Order not found";
            }
            
            // Get super admin order items
            List<SuperAdminOrderItem> orderItems = superAdminOrderItemService.findByOrderId(orderId);
            
            // Get system settings
            SystemSettings settings = systemSettingsService.getSystemSettings();
            
            StringBuilder receipt = new StringBuilder();
            
            // Business name (double width/height for emphasis)
            String businessName = settings.getBusinessName() != null && !settings.getBusinessName().trim().isEmpty()
                ? settings.getBusinessName().toUpperCase()
                : "KUMARA ENTERPRISES";
            receipt.append((char) 0x1B).append("!").append((char) 0x30); // ESC ! 0x30 = double width & height
            receipt.append(centerText(businessName, 26)).append("\n");
            receipt.append((char) 0x1B).append("!").append((char) 0x00); // Reset to normal size
            
            // Address
            receipt.append(centerText("No 58k Gagabada Rd,", 48)).append("\n");
            receipt.append(centerText("Wewala,Piliyandala", 48)).append("\n");
            
            // Contact numbers
            if (settings.getContactNumber() != null && !settings.getContactNumber().trim().isEmpty()) {
                receipt.append(centerText(settings.getContactNumber(), 48)).append("\n");
            } else {
                receipt.append(centerText("077 781 5955 / 011 261 3606", 48)).append("\n");
            }
            
            receipt.append("\n");
            
            // Invoice number and date
            String invoiceLine = String.format("Invoice                 %s",
                orderDetail.getIssuedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            receipt.append(invoiceLine).append("\n");
            
            String codeLine = String.format("%-24d%s",
                orderDetail.getCode(),
                orderDetail.getIssuedDate().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            receipt.append(codeLine).append("\n");
            
            // Customer name
            receipt.append(String.format("Customer : %s\n", orderDetail.getCustomerName()));
            
            receipt.append("................................................\n");
            
            // Items header
            receipt.append(String.format("%-14s %9s %5s %6s %9s\n",
                "Item", "Price", "Qty", "Disc", "Total"));
            receipt.append("................................................\n");
            
            // Items
            for (SuperAdminOrderItem item : orderItems) {
                String itemName = item.getProductName();
                if (itemName.length() > 14) {
                    itemName = itemName.substring(0, 11) + "...";
                }
                
                double discount = item.getDiscountPerUnit() != null ? item.getDiscountPerUnit() : 0.0;
                
                Double qty = item.getQuantity();
                String qtyStr;
                if (qty != null) {
                    if (qty == qty.intValue()) {
                        qtyStr = String.format("x%-3d", qty.intValue());
                    } else {
                        qtyStr = String.format("x%-5.2f", qty);
                    }
                } else {
                    qtyStr = "x0  ";
                }
                receipt.append(String.format("%-14s %9.1f %s %6.2f %9.2f\n",
                    itemName,
                    item.getUnitPrice(),
                    qtyStr,
                    discount,
                    item.getLineTotal()
                ));
            }
            
            receipt.append("................................................\n");
            
            // Totals section
            double subtotal = orderItems.stream().mapToDouble(item -> {
                Double qty = item.getQuantity();
                return item.getUnitPrice() * (qty != null ? qty : 0.0);
            }).sum();
            double totalDiscount = orderDetail.getDiscount();
            
            receipt.append(String.format("%-30s %17.2f\n", "Subtotal", subtotal));
            receipt.append(String.format("%-30s %17.2f\n", "Total Discount", totalDiscount));
            receipt.append(String.format("%-30s %17.2f\n", "Items", (double)orderItems.size()));
            receipt.append("------------------------------------------------\n");
            receipt.append(String.format("%-30s %17.2f\n", "TOTAL", orderDetail.getTotalCost()));
            
            // Customer Paid
            double customerPaid = orderDetail.getCustomerPaid() != null ? orderDetail.getCustomerPaid() : orderDetail.getTotalCost();
            receipt.append(String.format("%-30s %17.2f\n", "Customer Paid", customerPaid));
            
            // Payment method
            receipt.append(String.format("%-30s %17s\n", 
                "Payment: " + orderDetail.getPaymentMethod(), ""));
            
            // Change
            if ("PAID".equals(orderDetail.getPaymentStatus()) && customerPaid > orderDetail.getTotalCost()) {
                double change = customerPaid - orderDetail.getTotalCost();
                receipt.append(String.format("%-30s %17.2f\n", "Change", change));
            }
            
            receipt.append("------------------------------------------------\n");
            
            // Order type
//            String orderType = orderDetail.getOrderType() != null && orderDetail.getOrderType().equals("CONSTRUCTION")
//                ? "Construction" : "Hardware";
//            receipt.append(String.format("Type: %s\n", orderType));
//
//            receipt.append("------------------------------------------------\n");
//
            // Balance
            double balance = orderDetail.getBalance() != null ? orderDetail.getBalance() : 0.00;
            if (balance != 0.00) {
                receipt.append(String.format("%-30s %17.2f\n", "Balance", balance));
            } else {
                receipt.append(String.format("%-30s %17.2f\n", "Balance", 0.00));
            }
            receipt.append("................................................\n");
            
            // Footer message
            String footerMessage = settings.getFooterMessage() != null && !settings.getFooterMessage().trim().isEmpty()
                ? settings.getFooterMessage()
                : "Thank you for your business!";
            receipt.append(centerText(footerMessage, 48)).append("\n");
            receipt.append("................................................\n");
            
            // Software info
            receipt.append(centerText("Green Code Solutions", 48)).append("\n");
            receipt.append(centerText("078 150 8252 | 076 724 3647", 48)).append("\n");
            
            return receipt.toString();
            
        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating super admin receipt: " + e.getMessage();
        }
    }
    
    /**
     * Center text for thermal printer (48 characters wide for 80mm)
     */
    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }
    
    /**
     * Generate bill receipt PDF for a specific super admin order
     */
    public String generateSuperAdminBillReceipt(Long orderId) throws IOException {
        // Get super admin order details
        SuperAdminOrderDetail orderDetail = superAdminOrderDetailService.findSuperAdminOrderDetail(orderId);
        if (orderDetail == null) {
            throw new RuntimeException("Super Admin Order not found: " + orderId);
        }
        
        // Get super admin order items
        List<SuperAdminOrderItem> orderItems = superAdminOrderItemService.findByOrderId(orderId);
        
        // Create directory structure: ~/POS_Receipts/SuperAdmin/[CustomerName]/
        String userHome = System.getProperty("user.home");
        String operatorEmail = orderDetail.getOperatorEmail();
        
        // Extract operator username from email
        String operatorName = operatorEmail != null && operatorEmail.contains("@") 
            ? operatorEmail.substring(0, operatorEmail.indexOf("@")) 
            : (operatorEmail != null ? operatorEmail : "unknown");
        
        // Sanitize operator name for file system
        operatorName = operatorName.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        // Get customer name - use "Guest" if null or empty
        String customerName = orderDetail.getCustomerName();
        if (customerName == null || customerName.trim().isEmpty() || "Guest".equalsIgnoreCase(customerName.trim())) {
            customerName = "Guest";
        }
        
        // Sanitize customer name for file system
        customerName = customerName.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        // Create nested directory path: ~/POS_Receipts/SuperAdmin/[OperatorName]/[CustomerName]/
        String receiptsDir = userHome + File.separator + "POS_Receipts" + File.separator + "SuperAdmin" + File.separator + operatorName + File.separator + customerName;
        File directory = new File(receiptsDir);
        
        // Create directory if it doesn't exist
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                System.err.println("Failed to create receipts directory: " + receiptsDir);
                // Fallback to Downloads folder
                receiptsDir = userHome + File.separator + "Downloads";
            }
        }
        
        // Create file name and path
        String fileName = "SuperAdmin_Receipt_" + orderDetail.getCode() + "_" + System.currentTimeMillis() + ".pdf";
        String filePath = receiptsDir + File.separator + fileName;
        
        // Create PDF
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.setMargins(20, 20, 20, 20);
        
        // Get system settings
        SystemSettings settings = systemSettingsService.getSystemSettings();
        
        // Store/Company Header
        String businessName = settings.getBusinessName() != null && !settings.getBusinessName().trim().isEmpty()
            ? settings.getBusinessName().toUpperCase()
            : "KUMARA ENTERPRISES";
        Paragraph storeName = new Paragraph(businessName)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(22)
            .setBold()
            .setMarginBottom(5);
        document.add(storeName);
        
        // Store Address
        StringBuilder addressText = new StringBuilder();
        if (settings.getAddress() != null && !settings.getAddress().trim().isEmpty()) {
            addressText.append(settings.getAddress());
            if (!settings.getAddress().toLowerCase().contains("wewala")) {
                addressText.append("\nWewala,Piliyandala");
            }
        } else {
            addressText.append("58k Gagabada Rd,");
            addressText.append("\nWewala,Piliyandala");
        }
        
        if (settings.getContactNumber() != null && !settings.getContactNumber().trim().isEmpty()) {
            if (addressText.length() > 0) {
                addressText.append("\n");
            }
            addressText.append(settings.getContactNumber());
        } else {
            if (addressText.length() > 0) {
                addressText.append("\n");
            }
            addressText.append("077 781 5955 / 011 261 3606");
        }
        
        Paragraph storeAddress = new Paragraph(addressText.toString())
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10)
            .setMarginBottom(15);
        document.add(storeAddress);
        
        // Super Admin Order Label
        Paragraph superAdminLabel = new Paragraph("*** SUPER ADMIN ORDER ***")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(14)
            .setBold()
            .setMarginBottom(10);
        document.add(superAdminLabel);
        
        // Divider line
        document.add(new Paragraph("=" .repeat(50))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10)
            .setMarginBottom(10));
        
        // Receipt Title
        Paragraph receiptTitle = new Paragraph("SALES RECEIPT")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(14)
            .setBold()
            .setMarginBottom(15);
        document.add(receiptTitle);
        
        // Order Information
        document.add(new Paragraph("Receipt No: " + orderDetail.getCode())
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph("Date: " + orderDetail.getIssuedDate().format(RECEIPT_DATE_FORMATTER))
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph("Customer: " + orderDetail.getCustomerName())
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph("Operator: " + orderDetail.getOperatorEmail())
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph("Payment Method: " + orderDetail.getPaymentMethod())
            .setFontSize(10)
            .setMarginBottom(3));
        
        // Order Type
        String orderTypeDisplay = orderDetail.getOrderType() != null && orderDetail.getOrderType().equals("CONSTRUCTION") 
            ? "Construction" 
            : "Hardware";
        document.add(new Paragraph("Order Type: " + orderTypeDisplay)
            .setFontSize(10)
            .setMarginBottom(10));
        
        // Divider line
        document.add(new Paragraph("-" .repeat(50))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10)
            .setMarginBottom(10));
        
        // Items header
        Paragraph itemsHeader = new Paragraph(String.format("%-14s %9s %5s %6s %9s",
            "Item", "Price", "Qty", "Disc", "Total"))
            .setFontSize(10)
            .setBold()
            .setMarginBottom(3);
        document.add(itemsHeader);
        
        // Divider line
        document.add(new Paragraph("." .repeat(48))
            .setFontSize(10)
            .setMarginBottom(5));
        
        // Items
        for (SuperAdminOrderItem item : orderItems) {
            String itemName = item.getProductName();
            if (itemName.length() > 14) {
                itemName = itemName.substring(0, 11) + "...";
            }
            
            double discount = item.getDiscountPerUnit() != null ? item.getDiscountPerUnit() : 0.0;
            
            Double qty = item.getQuantity();
            String qtyStr;
            if (qty != null) {
                if (qty == qty.intValue()) {
                    qtyStr = String.format("x%-3d", qty.intValue());
                } else {
                    qtyStr = String.format("x%-5.2f", qty);
                }
            } else {
                qtyStr = "x0  ";
            }
            
            Paragraph itemLine = new Paragraph(String.format("%-14s %9.1f %s %6.2f %9.2f",
                itemName,
                item.getUnitPrice(),
                qtyStr,
                discount,
                item.getLineTotal()))
                .setFontSize(9)
                .setFont(PdfFontFactory.createFont(StandardFonts.COURIER))
                .setMarginBottom(2);
            document.add(itemLine);
        }
        
        // Divider line after items
        document.add(new Paragraph("." .repeat(48))
            .setFontSize(10)
            .setMarginBottom(10));
        
        // Divider line
        document.add(new Paragraph("-" .repeat(50))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10)
            .setMarginBottom(10));
        
        // Total section
        double subtotal = orderItems.stream().mapToDouble(item -> 
            item.getUnitPrice() * item.getQuantity()).sum();
        double totalDiscount = orderDetail.getDiscount();
        
        // Use monospace font for proper alignment
        PdfFont monospaceFont = PdfFontFactory.createFont(StandardFonts.COURIER);
        
        document.add(new Paragraph(String.format("%-30s %17.2f", "Subtotal", subtotal))
            .setFont(monospaceFont)
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph(String.format("%-30s %17.2f", "Total Discount", totalDiscount))
            .setFont(monospaceFont)
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph(String.format("%-30s %17.2f", "Items", (double)orderItems.size()))
            .setFont(monospaceFont)
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph("-" .repeat(48))
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph(String.format("%-30s %17.2f", "TOTAL", orderDetail.getTotalCost()))
            .setFont(monospaceFont)
            .setFontSize(12)
            .setBold()
            .setMarginBottom(3));
        
        // Customer Paid
        double customerPaid = orderDetail.getCustomerPaid() != null ? orderDetail.getCustomerPaid() : orderDetail.getTotalCost();
        document.add(new Paragraph(String.format("%-30s %17.2f", "Customer Paid", customerPaid))
            .setFont(monospaceFont)
            .setFontSize(10)
            .setMarginBottom(3));
        
        // Payment method
        document.add(new Paragraph(String.format("%-30s %17s", "Payment: " + orderDetail.getPaymentMethod(), ""))
            .setFont(monospaceFont)
            .setFontSize(10)
            .setMarginBottom(3));
        
        // Change
        if ("PAID".equals(orderDetail.getPaymentStatus()) && customerPaid > orderDetail.getTotalCost()) {
            double change = customerPaid - orderDetail.getTotalCost();
            document.add(new Paragraph(String.format("%-30s %17.2f", "Change", change))
                .setFont(monospaceFont)
                .setFontSize(10)
                .setMarginBottom(3));
        }
        
        document.add(new Paragraph("-" .repeat(48))
            .setFontSize(10)
            .setMarginBottom(3));
        
        // Order Type
        String orderType = orderDetail.getOrderType() != null && orderDetail.getOrderType().equals("CONSTRUCTION") 
            ? "Construction" : "Hardware";
        document.add(new Paragraph(String.format("Type: %s", orderType))
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph("-" .repeat(48))
            .setFontSize(10)
            .setMarginBottom(3));
        
        // Balance
        double balance = orderDetail.getBalance() != null ? orderDetail.getBalance() : 0.00;
        document.add(new Paragraph(String.format("%-30s %17.2f", "Balance", balance))
            .setFont(monospaceFont)
            .setFontSize(10)
            .setBold()
            .setMarginBottom(5));
        
        // Payment Status
        String paymentStatusText = "PAID".equals(orderDetail.getPaymentStatus()) ? 
            "*** PAID ***" : "*** PAYMENT PENDING ***";
        document.add(new Paragraph(paymentStatusText)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(12)
            .setBold()
            .setMarginBottom(20));
        
        // Footer
        String footerMessage = settings.getFooterMessage() != null && !settings.getFooterMessage().trim().isEmpty()
            ? settings.getFooterMessage()
            : "Thank you for your business!";
        document.add(new Paragraph(footerMessage)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(11)
            .setMarginBottom(10));
        
        document.add(new Paragraph("=" .repeat(50))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10));
        
        document.close();
        return filePath;
    }
}

