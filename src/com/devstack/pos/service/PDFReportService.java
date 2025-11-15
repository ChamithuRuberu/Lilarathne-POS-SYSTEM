package com.devstack.pos.service;

import com.devstack.pos.entity.*;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PDFReportService {
    
    private final OrderDetailService orderDetailService;
    private final ReturnOrderService returnOrderService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final SupplierService supplierService;
    private final ProductDetailService productDetailService;
    private final OrderItemService orderItemService;
    private final SystemSettingsService systemSettingsService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    /**
     * Generate bill receipt PDF for a specific order
     */
    public String generateBillReceipt(Long orderId) throws FileNotFoundException {
        // Get order details
        OrderDetail orderDetail = orderDetailService.findOrderDetail(orderId);
        if (orderDetail == null) {
            throw new RuntimeException("Order not found: " + orderId);
        }
        
        // Get order items
        List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
        
        // Create directory structure: ~/POS_Receipts/[CashierName]/[CustomerName]/
        String userHome = System.getProperty("user.home");
        String cashierEmail = orderDetail.getOperatorEmail();
        
        // Extract cashier username from email (everything before @)
        String cashierName = cashierEmail != null && cashierEmail.contains("@") 
            ? cashierEmail.substring(0, cashierEmail.indexOf("@")) 
            : (cashierEmail != null ? cashierEmail : "unknown");
        
        // Sanitize cashier name for file system (remove special characters)
        cashierName = cashierName.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        // Get customer name - use "Guest" if null or empty
        String customerName = orderDetail.getCustomerName();
        if (customerName == null || customerName.trim().isEmpty() || "Guest".equalsIgnoreCase(customerName.trim())) {
            customerName = "Guest";
        }
        
        // Sanitize customer name for file system (remove special characters)
        customerName = customerName.replaceAll("[^a-zA-Z0-9_-]", "_");
        
        // Create nested directory path: ~/POS_Receipts/[CashierName]/[CustomerName]/
        String receiptsDir = userHome + File.separator + "POS_Receipts" + File.separator + cashierName + File.separator + customerName;
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
        String fileName = "Receipt_" + orderDetail.getCode() + "_" + System.currentTimeMillis() + ".pdf";
        String filePath = receiptsDir + File.separator + fileName;
        
        // Create PDF
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        
        // Use smaller page size for receipt (thermal printer format)
        // PageSize customSize = new PageSize(226.77f, 566.93f); // 80mm width, 200mm height
        Document document = new Document(pdf);
        document.setMargins(20, 20, 20, 20);
        
        // Get system settings
        SystemSettings settings = systemSettingsService.getSystemSettings();
        
        // Store/Company Header - use business name from settings
        String businessName = settings.getBusinessName() != null && !settings.getBusinessName().trim().isEmpty()
            ? settings.getBusinessName().toUpperCase()
            : "LILARATHNE POS SYSTEM";
        Paragraph storeName = new Paragraph(businessName)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(18)
            .setBold()
            .setMarginBottom(5);
        document.add(storeName);
        
        // Store Address - use address and contact from settings
        StringBuilder addressText = new StringBuilder();
        if (settings.getAddress() != null && !settings.getAddress().trim().isEmpty()) {
            addressText.append(settings.getAddress());
        }
        if (settings.getContactNumber() != null && !settings.getContactNumber().trim().isEmpty()) {
            if (addressText.length() > 0) {
                addressText.append("\n");
            }
            addressText.append("Tel: ").append(settings.getContactNumber());
        }
        if (addressText.length() == 0) {
            addressText.append("123 Main Street, Colombo\nTel: +94 11 234 5678"); // Default fallback
        }
        
        Paragraph storeAddress = new Paragraph(addressText.toString())
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10)
            .setMarginBottom(15);
        document.add(storeAddress);
        
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
        
        document.add(new Paragraph("Cashier: " + orderDetail.getOperatorEmail())
            .setFontSize(10)
            .setMarginBottom(3));
        
        document.add(new Paragraph("Payment Method: " + orderDetail.getPaymentMethod())
            .setFontSize(10)
            .setMarginBottom(10));
        
        // Divider line
        document.add(new Paragraph("-" .repeat(50))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10)
            .setMarginBottom(10));
        
        // Items Table
        Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{3, 1, 2, 2}))
            .useAllAvailableWidth()
            .setMarginBottom(10);
        
        // Table headers
        itemsTable.addHeaderCell(new Cell().add(new Paragraph("Item").setBold().setFontSize(10)));
        itemsTable.addHeaderCell(new Cell().add(new Paragraph("Qty").setBold().setFontSize(10)));
        itemsTable.addHeaderCell(new Cell().add(new Paragraph("Price").setBold().setFontSize(10)));
        itemsTable.addHeaderCell(new Cell().add(new Paragraph("Total").setBold().setFontSize(10)));
        
        // Table rows
        for (OrderItem item : orderItems) {
            itemsTable.addCell(new Cell().add(new Paragraph(item.getProductName()).setFontSize(9)));
            itemsTable.addCell(new Cell().add(new Paragraph(String.valueOf(item.getQuantity())).setFontSize(9)));
            itemsTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", item.getUnitPrice())).setFontSize(9)));
            itemsTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", item.getLineTotal())).setFontSize(9)));
            
            // If there's a discount, show it
            if (item.getDiscountPerUnit() != null && item.getDiscountPerUnit() > 0) {
                itemsTable.addCell(new Cell(1, 4)
                    .add(new Paragraph("   Discount: -" + String.format("%.2f", item.getTotalDiscount()))
                    .setFontSize(8)
                    .setFontColor(ColorConstants.RED)));
            }
        }
        
        document.add(itemsTable);
        
        // Divider line
        document.add(new Paragraph("-" .repeat(50))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10)
            .setMarginBottom(10));
        
        // Total section
        double subtotal = orderItems.stream().mapToDouble(item -> 
            item.getUnitPrice() * item.getQuantity()).sum();
        double totalDiscount = orderDetail.getDiscount();
        
        document.add(new Paragraph("Subtotal: LKR " + String.format("%.2f", subtotal))
            .setTextAlignment(TextAlignment.RIGHT)
            .setFontSize(11)
            .setMarginBottom(3));
        
        if (totalDiscount > 0) {
            document.add(new Paragraph("Total Discount: -LKR " + String.format("%.2f", totalDiscount))
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(11)
                .setFontColor(ColorConstants.RED)
                .setMarginBottom(3));
        }
        
        document.add(new Paragraph("=" .repeat(50))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10)
            .setMarginBottom(5));
        
        document.add(new Paragraph("TOTAL: LKR " + String.format("%.2f", orderDetail.getTotalCost()))
            .setTextAlignment(TextAlignment.RIGHT)
            .setFontSize(14)
            .setBold()
            .setMarginBottom(15));
        
        // Payment Status
        String paymentStatusText = "PAID".equals(orderDetail.getPaymentStatus()) ? 
            "*** PAID ***" : "*** PAYMENT PENDING ***";
        document.add(new Paragraph(paymentStatusText)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(12)
            .setBold()
            .setMarginBottom(20));
        
        // Footer - use footer message from settings
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
    
    /**
     * Generate comprehensive sales report PDF
     */
    public String generateSalesReportPDF(LocalDateTime startDate, LocalDateTime endDate, String reportType) 
            throws FileNotFoundException {
        String fileName = "Sales_Report_" + System.currentTimeMillis() + ".pdf";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Header
        addHeader(document, "Sales Report", startDate, endDate);
        
        // Summary Statistics
        addSummarySection(document, startDate, endDate);
        
        // Sales by Period
        addSalesByPeriodSection(document, startDate, endDate, reportType);
        
        // Top Products
        addTopProductsSection(document, startDate, endDate);
        
        // Sales by Category
        addSalesByCategorySection(document, startDate, endDate);
        
        // Footer
        addFooter(document);
        
        document.close();
        return filePath;
    }
    
    /**
     * Generate comprehensive return orders report PDF
     */
    public String generateReturnOrdersReportPDF(LocalDateTime startDate, LocalDateTime endDate) 
            throws FileNotFoundException {
        String fileName = "Return_Orders_Report_" + System.currentTimeMillis() + ".pdf";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Header
        addHeader(document, "Return Orders Report", startDate, endDate);
        
        // Return Statistics
        addReturnStatisticsSection(document, startDate, endDate);
        
        // Return Details
        addReturnDetailsSection(document, startDate, endDate);
        
        // Footer
        addFooter(document);
        
        document.close();
        return filePath;
    }
    
    /**
     * Generate comprehensive inventory report PDF
     */
    public String generateInventoryReportPDF() throws FileNotFoundException {
        String fileName = "Inventory_Report_" + System.currentTimeMillis() + ".pdf";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Header
        addHeader(document, "Inventory Report", null, null);
        
        // Inventory Summary
        addInventorySummarySection(document);
        
        // Low Stock Items
        addLowStockSection(document);
        
        // Product Details
        addProductDetailsSection(document);
        
        // Footer
        addFooter(document);
        
        document.close();
        return filePath;
    }
    
    /**
     * Generate comprehensive financial report PDF
     */
    public String generateFinancialReportPDF(LocalDateTime startDate, LocalDateTime endDate) 
            throws FileNotFoundException {
        String fileName = "Financial_Report_" + System.currentTimeMillis() + ".pdf";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Header
        addHeader(document, "Financial Report", startDate, endDate);
        
        // Financial Summary
        addFinancialSummarySection(document, startDate, endDate);
        
        // Revenue vs Returns
        addRevenueVsReturnsSection(document, startDate, endDate);
        
        // Top Customers
        addTopCustomersSection(document, startDate, endDate);
        
        // Footer
        addFooter(document);
        
        document.close();
        return filePath;
    }
    
    /**
     * Generate comprehensive supplier report PDF
     */
    public String generateSupplierReportPDF() throws FileNotFoundException {
        String fileName = "Supplier_Report_" + System.currentTimeMillis() + ".pdf";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Header
        addHeader(document, "Supplier & Purchase Orders Report", null, null);
        
        // Supplier Summary
        addSupplierSummarySection(document);
        
        // Purchase Orders
        addPurchaseOrdersSection(document);
        
        // Footer
        addFooter(document);
        
        document.close();
        return filePath;
    }
    
    /**
     * Generate comprehensive all-in-one report PDF
     */
    public String generateComprehensiveReportPDF(LocalDateTime startDate, LocalDateTime endDate) 
            throws FileNotFoundException {
        String fileName = "Comprehensive_POS_Report_" + System.currentTimeMillis() + ".pdf";
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Header
        addHeader(document, "Comprehensive POS System Report", startDate, endDate);
        
        // Executive Summary
        addExecutiveSummarySection(document, startDate, endDate);
        
        // Sales Analysis
        addSalesAnalysisSection(document, startDate, endDate);
        
        // Return Orders Analysis
        addReturnOrdersAnalysisSection(document, startDate, endDate);
        
        // Inventory Status
        addInventoryStatusSection(document);
        
        // Customer Analysis
        addCustomerAnalysisSection(document, startDate, endDate);
        
        // Supplier Analysis
        addSupplierAnalysisSection(document);
        
        // Footer
        addFooter(document);
        
        document.close();
        return filePath;
    }
    
    // Helper methods for adding sections
    
    private void addHeader(Document document, String title, LocalDateTime startDate, LocalDateTime endDate) {
        // Get system settings for header
        SystemSettings settings = systemSettingsService.getSystemSettings();
        String businessName = settings.getBusinessName() != null && !settings.getBusinessName().trim().isEmpty()
            ? settings.getBusinessName()
            : "Lilarathne POS System";
        
        Paragraph businessHeader = new Paragraph(businessName.toUpperCase())
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(businessHeader);
        
        if (settings.getAddress() != null && !settings.getAddress().trim().isEmpty()) {
            Paragraph address = new Paragraph(settings.getAddress())
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2);
            document.add(address);
        }
        
        Paragraph header = new Paragraph(title)
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10)
                .setMarginBottom(10);
        document.add(header);
        
        if (startDate != null && endDate != null) {
            long daysBetween = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate());
            Paragraph dateRange = new Paragraph(
                    "Period: " + startDate.format(DATE_ONLY_FORMATTER) + " to " + endDate.format(DATE_ONLY_FORMATTER) + 
                    " (" + daysBetween + " days)")
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            document.add(dateRange);
        }
        
        Paragraph generatedDate = new Paragraph(
                "Generated on: " + LocalDateTime.now().format(DATE_FORMATTER))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(generatedDate);
        
        document.add(new Paragraph().setMarginBottom(10));
    }
    
    /**
     * Calculate previous period dates for comparison
     */
    private LocalDateTime[] calculatePreviousPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return new LocalDateTime[]{null, null};
        }
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDateTime prevStartDate = startDate.minusDays(daysBetween + 1);
        LocalDateTime prevEndDate = startDate.minusDays(1);
        return new LocalDateTime[]{prevStartDate, prevEndDate};
    }
    
    /**
     * Calculate growth percentage
     */
    private double calculateGrowthPercentage(double current, double previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((current - previous) / previous) * 100.0;
    }
    
    /**
     * Format growth indicator with arrow
     */
    private String formatGrowthIndicator(double current, double previous) {
        double growth = calculateGrowthPercentage(current, previous);
        String arrow = growth > 0 ? "↑" : (growth < 0 ? "↓" : "→");
        return String.format("%s %.2f%%", arrow, Math.abs(growth));
    }
    
    private void addSummarySection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("📊 KEY PERFORMANCE INDICATORS (KPIs)")
                .setFontSize(18)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(15));
        
        // Current period data
        Double revenue = startDate != null && endDate != null 
                ? orderDetailService.getRevenueByDateRange(startDate, endDate)
                : orderDetailService.getTotalRevenue();
        Long orders = startDate != null && endDate != null
                ? orderDetailService.countOrdersByDateRange(startDate, endDate)
                : orderDetailService.getTotalOrderCount();
        Double avgOrder = startDate != null && endDate != null
                ? orderDetailService.getAverageOrderValueByDateRange(startDate, endDate)
                : orderDetailService.getAverageOrderValue();
        
        // Get returns data
        Double refunds = startDate != null && endDate != null
                ? returnOrderService.getTotalRefundAmountByDateRange(startDate, endDate)
                : returnOrderService.getTotalRefundAmount();
        Long returns = startDate != null && endDate != null
                ? returnOrderService.countReturnsByDateRange(startDate, endDate)
                : (long) returnOrderService.findAllReturnOrders().size();
        
        Double netRevenue = (revenue != null ? revenue : 0.0) - (refunds != null ? refunds : 0.0);
        
        // Calculate previous period for growth indicators
        LocalDateTime[] prevPeriod = calculatePreviousPeriod(startDate, endDate);
        Double prevRevenue = null;
        Long prevOrders = null;
        Double prevAvgOrder = null;
        Double prevRefunds = null;
        Long prevReturns = null;
        
        if (prevPeriod[0] != null && prevPeriod[1] != null) {
            prevRevenue = orderDetailService.getRevenueByDateRange(prevPeriod[0], prevPeriod[1]);
            prevOrders = orderDetailService.countOrdersByDateRange(prevPeriod[0], prevPeriod[1]);
            prevAvgOrder = orderDetailService.getAverageOrderValueByDateRange(prevPeriod[0], prevPeriod[1]);
            prevRefunds = returnOrderService.getTotalRefundAmountByDateRange(prevPeriod[0], prevPeriod[1]);
            prevReturns = returnOrderService.countReturnsByDateRange(prevPeriod[0], prevPeriod[1]);
        }
        
        // Create enhanced summary table with growth indicators
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 1.5F}));
        summaryTable.setWidth(UnitValue.createPercentValue(100));
        
        // Header row
        summaryTable.addHeaderCell(new Cell().add(new Paragraph("Metric").setBold()).setPadding(8));
        summaryTable.addHeaderCell(new Cell().add(new Paragraph("Current Period").setBold()).setPadding(8));
        summaryTable.addHeaderCell(new Cell().add(new Paragraph("Growth").setBold()).setPadding(8));
        
        // Revenue row with growth
        double revenueVal = revenue != null ? revenue : 0.0;
        double prevRevenueVal = prevRevenue != null ? prevRevenue : 0.0;
        String revenueGrowth = prevPeriod[0] != null ? formatGrowthIndicator(revenueVal, prevRevenueVal) : "N/A";
        summaryTable.addCell(new Cell().add(new Paragraph("💰 Total Revenue")).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", revenueVal))).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(revenueGrowth)).setPadding(8));
        
        // Net Revenue
        double prevNetRevenue = (prevRevenueVal) - (prevRefunds != null ? prevRefunds : 0.0);
        String netRevenueGrowth = prevPeriod[0] != null ? formatGrowthIndicator(netRevenue, prevNetRevenue) : "N/A";
        summaryTable.addCell(new Cell().add(new Paragraph("💵 Net Revenue")).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", netRevenue))).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(netRevenueGrowth)).setPadding(8));
        
        // Orders row with growth
        long ordersVal = orders != null ? orders : 0;
        long prevOrdersVal = prevOrders != null ? prevOrders : 0;
        String ordersGrowth = prevPeriod[0] != null ? formatGrowthIndicator(ordersVal, prevOrdersVal) : "N/A";
        summaryTable.addCell(new Cell().add(new Paragraph("📦 Total Orders")).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(String.valueOf(ordersVal))).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(ordersGrowth)).setPadding(8));
        
        // Average Order Value
        double avgOrderVal = avgOrder != null ? avgOrder : 0.0;
        double prevAvgOrderVal = prevAvgOrder != null ? prevAvgOrder : 0.0;
        String avgOrderGrowth = prevPeriod[0] != null ? formatGrowthIndicator(avgOrderVal, prevAvgOrderVal) : "N/A";
        summaryTable.addCell(new Cell().add(new Paragraph("📈 Avg Order Value")).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", avgOrderVal))).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(avgOrderGrowth)).setPadding(8));
        
        // Returns
        long returnsVal = returns != null ? returns : 0;
        long prevReturnsVal = prevReturns != null ? prevReturns : 0;
        String returnsGrowth = prevPeriod[0] != null ? formatGrowthIndicator(returnsVal, prevReturnsVal) : "N/A";
        summaryTable.addCell(new Cell().add(new Paragraph("🔄 Total Returns")).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(String.valueOf(returnsVal))).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(returnsGrowth)).setPadding(8));
        
        // Refund Amount
        double refundsVal = refunds != null ? refunds : 0.0;
        double prevRefundsVal = prevRefunds != null ? prevRefunds : 0.0;
        String refundsGrowth = prevPeriod[0] != null ? formatGrowthIndicator(refundsVal, prevRefundsVal) : "N/A";
        summaryTable.addCell(new Cell().add(new Paragraph("💸 Total Refunds")).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", refundsVal))).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(refundsGrowth)).setPadding(8));
        
        // Return Rate (returns as percentage of orders)
        double returnRate = ordersVal > 0 ? (returnsVal * 100.0 / ordersVal) : 0.0;
        double prevReturnRate = prevOrdersVal > 0 ? (prevReturnsVal * 100.0 / prevOrdersVal) : 0.0;
        String returnRateGrowth = prevPeriod[0] != null ? formatGrowthIndicator(returnRate, prevReturnRate) : "N/A";
        summaryTable.addCell(new Cell().add(new Paragraph("📉 Return Rate")).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(String.format("%.2f%%", returnRate))).setPadding(8));
        summaryTable.addCell(new Cell().add(new Paragraph(returnRateGrowth)).setPadding(8));
        
        document.add(summaryTable);
        
        // Add insights paragraph
        if (prevPeriod[0] != null) {
            document.add(new Paragraph().setMarginTop(10));
            Paragraph insights = new Paragraph("📌 Insights: " + generateInsights(revenueVal, prevRevenueVal, ordersVal, prevOrdersVal, returnRate, prevReturnRate))
                    .setFontSize(10)
                    .setItalic()
                    .setMarginBottom(15);
            document.add(insights);
        } else {
            document.add(new Paragraph().setMarginBottom(15));
        }
    }
    
    /**
     * Generate insights based on KPIs
     */
    private String generateInsights(double revenue, double prevRevenue, long orders, long prevOrders, double returnRate, double prevReturnRate) {
        StringBuilder insights = new StringBuilder();
        
        if (revenue > prevRevenue) {
            insights.append("Revenue is growing. ");
        } else if (revenue < prevRevenue) {
            insights.append("Revenue has declined. ");
        }
        
        if (orders > prevOrders) {
            insights.append("Order volume increased. ");
        } else if (orders < prevOrders) {
            insights.append("Order volume decreased. ");
        }
        
        if (returnRate > prevReturnRate) {
            insights.append("⚠️ Return rate increased - review product quality. ");
        } else if (returnRate < prevReturnRate) {
            insights.append("✓ Return rate improved. ");
        }
        
        if (insights.length() == 0) {
            insights.append("Performance is stable.");
        }
        
        return insights.toString();
    }
    
    private void addSummaryRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()).setPadding(5));
        table.addCell(new Cell().add(new Paragraph(value)).setPadding(5));
    }
    
    private void addSalesByPeriodSection(Document document, LocalDateTime startDate, LocalDateTime endDate, String reportType) {
        document.add(new Paragraph("📅 SALES BY PERIOD")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        if (startDate == null || endDate == null) {
            document.add(new Paragraph("Select a date range to view sales by period.")
                    .setFontSize(10)
                    .setItalic());
            document.add(new Paragraph().setMarginBottom(15));
            return;
        }
        
        // Calculate daily sales breakdown
        List<OrderDetail> orders = orderDetailService.findOrdersByDateRange(startDate, endDate);
        Map<LocalDate, Double> dailySales = orders.stream()
                .filter(o -> "PAID".equals(o.getPaymentStatus()))
                .collect(Collectors.groupingBy(
                    o -> o.getIssuedDate().toLocalDate(),
                    Collectors.summingDouble(OrderDetail::getTotalCost)
                ));
        
        if (dailySales.isEmpty()) {
            document.add(new Paragraph("No sales data available for the selected period.")
                    .setFontSize(10)
                    .setItalic());
            document.add(new Paragraph().setMarginBottom(15));
            return;
        }
        
        // Create table with daily breakdown
        Table periodTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 1}));
        periodTable.setWidth(UnitValue.createPercentValue(100));
        
        periodTable.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()).setPadding(5));
        periodTable.addHeaderCell(new Cell().add(new Paragraph("Revenue").setBold()).setPadding(5));
        periodTable.addHeaderCell(new Cell().add(new Paragraph("Orders").setBold()).setPadding(5));
        
        Map<LocalDate, Long> dailyOrderCount = orders.stream()
                .collect(Collectors.groupingBy(
                    o -> o.getIssuedDate().toLocalDate(),
                    Collectors.counting()
                ));
        
        dailySales.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    LocalDate date = entry.getKey();
                    Double revenue = entry.getValue();
                    Long orderCount = dailyOrderCount.getOrDefault(date, 0L);
                    
                    periodTable.addCell(new Cell().add(new Paragraph(date.format(DATE_ONLY_FORMATTER))).setPadding(5));
                    periodTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", revenue))).setPadding(5));
                    periodTable.addCell(new Cell().add(new Paragraph(String.valueOf(orderCount))).setPadding(5));
                });
        
        document.add(periodTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addTopProductsSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("🏆 TOP SELLING PRODUCTS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        List<Object[]> topProducts;
        if (startDate != null && endDate != null) {
            topProducts = orderItemService.getTopSellingProductsWithRevenue(startDate, endDate);
        } else {
            topProducts = orderItemService.getTopSellingProductsWithRevenue();
        }
        
        if (topProducts.isEmpty()) {
            document.add(new Paragraph("No product sales data available.")
                    .setFontSize(10)
                    .setItalic());
            document.add(new Paragraph().setMarginBottom(15));
            return;
        }
        
        // Limit to top 10
        topProducts = topProducts.stream().limit(10).collect(Collectors.toList());
        
        Table productsTable = new Table(UnitValue.createPercentArray(new float[]{0.5F, 2, 1, 1.5F, 1.5F}));
        productsTable.setWidth(UnitValue.createPercentValue(100));
        
        productsTable.addHeaderCell(new Cell().add(new Paragraph("#").setBold()).setPadding(5));
        productsTable.addHeaderCell(new Cell().add(new Paragraph("Product Name").setBold()).setPadding(5));
        productsTable.addHeaderCell(new Cell().add(new Paragraph("Quantity").setBold()).setPadding(5));
        productsTable.addHeaderCell(new Cell().add(new Paragraph("Revenue").setBold()).setPadding(5));
        productsTable.addHeaderCell(new Cell().add(new Paragraph("Avg Price").setBold()).setPadding(5));
        
        int rank = 1;
        for (Object[] product : topProducts) {
            Integer productCode = ((Number) product[0]).intValue();
            String productName = (String) product[1];
            Integer quantity = ((Number) product[2]).intValue();
            Double revenue = ((Number) product[3]).doubleValue();
            Double avgPrice = quantity > 0 ? revenue / quantity : 0.0;
            
            productsTable.addCell(new Cell().add(new Paragraph(String.valueOf(rank++))).setPadding(5));
            productsTable.addCell(new Cell().add(new Paragraph(productName != null ? productName : "Unknown")).setPadding(5));
            productsTable.addCell(new Cell().add(new Paragraph(String.valueOf(quantity))).setPadding(5));
            productsTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", revenue))).setPadding(5));
            productsTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", avgPrice))).setPadding(5));
        }
        
        document.add(productsTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addSalesByCategorySection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("📂 SALES BY CATEGORY")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        List<Object[]> categorySales;
        if (startDate != null && endDate != null) {
            categorySales = orderItemService.getSalesByCategory(startDate, endDate);
        } else {
            categorySales = orderItemService.getSalesByCategory();
        }
        
        if (categorySales.isEmpty()) {
            document.add(new Paragraph("No category sales data available.")
                    .setFontSize(10)
                    .setItalic());
            document.add(new Paragraph().setMarginBottom(15));
            return;
        }
        
        // Calculate total revenue for percentage calculation
        double totalRevenue = categorySales.stream()
                .mapToDouble(cat -> ((Number) cat[2]).doubleValue())
                .sum();
        
        Table categoryTable = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1.5F, 1.5F}));
        categoryTable.setWidth(UnitValue.createPercentValue(100));
        
        categoryTable.addHeaderCell(new Cell().add(new Paragraph("Category").setBold()).setPadding(5));
        categoryTable.addHeaderCell(new Cell().add(new Paragraph("Orders").setBold()).setPadding(5));
        categoryTable.addHeaderCell(new Cell().add(new Paragraph("Revenue").setBold()).setPadding(5));
        categoryTable.addHeaderCell(new Cell().add(new Paragraph("% of Total").setBold()).setPadding(5));
        
        for (Object[] category : categorySales) {
            String categoryName = (String) category[0];
            Long orderCount = ((Number) category[1]).longValue();
            Double revenue = ((Number) category[2]).doubleValue();
            double percentage = totalRevenue > 0 ? (revenue / totalRevenue) * 100.0 : 0.0;
            
            categoryTable.addCell(new Cell().add(new Paragraph(categoryName != null ? categoryName : "Uncategorized")).setPadding(5));
            categoryTable.addCell(new Cell().add(new Paragraph(String.valueOf(orderCount))).setPadding(5));
            categoryTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", revenue))).setPadding(5));
            categoryTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", percentage))).setPadding(5));
        }
        
        document.add(categoryTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addReturnStatisticsSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("🔄 RETURN STATISTICS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        Long totalReturns = startDate != null && endDate != null
                ? returnOrderService.countReturnsByDateRange(startDate, endDate)
                : (long) returnOrderService.findAllReturnOrders().size();
        Long pendingReturns = returnOrderService.countByStatus("PENDING");
        Long completedReturns = returnOrderService.countByStatus("COMPLETED");
        Double totalRefunds = startDate != null && endDate != null
                ? returnOrderService.getTotalRefundAmountByDateRange(startDate, endDate)
                : returnOrderService.getTotalRefundAmount();
        
        // Calculate previous period for comparison
        LocalDateTime[] prevPeriod = calculatePreviousPeriod(startDate, endDate);
        Long prevTotalReturns = null;
        Double prevTotalRefunds = null;
        if (prevPeriod[0] != null && prevPeriod[1] != null) {
            prevTotalReturns = returnOrderService.countReturnsByDateRange(prevPeriod[0], prevPeriod[1]);
            prevTotalRefunds = returnOrderService.getTotalRefundAmountByDateRange(prevPeriod[0], prevPeriod[1]);
        }
        
        Table returnTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 1.5F}));
        returnTable.setWidth(UnitValue.createPercentValue(100));
        
        returnTable.addHeaderCell(new Cell().add(new Paragraph("Metric").setBold()).setPadding(8));
        returnTable.addHeaderCell(new Cell().add(new Paragraph("Current Period").setBold()).setPadding(8));
        returnTable.addHeaderCell(new Cell().add(new Paragraph("Growth").setBold()).setPadding(8));
        
        long totalReturnsVal = totalReturns != null ? totalReturns : 0;
        long prevTotalReturnsVal = prevTotalReturns != null ? prevTotalReturns : 0;
        String returnsGrowth = prevPeriod[0] != null ? formatGrowthIndicator(totalReturnsVal, prevTotalReturnsVal) : "N/A";
        returnTable.addCell(new Cell().add(new Paragraph("Total Returns")).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(String.valueOf(totalReturnsVal))).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(returnsGrowth)).setPadding(8));
        
        returnTable.addCell(new Cell().add(new Paragraph("Pending Returns")).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(String.valueOf(pendingReturns != null ? pendingReturns : 0))).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph("-")).setPadding(8));
        
        returnTable.addCell(new Cell().add(new Paragraph("Completed Returns")).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(String.valueOf(completedReturns != null ? completedReturns : 0))).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph("-")).setPadding(8));
        
        double totalRefundsVal = totalRefunds != null ? totalRefunds : 0.0;
        double prevTotalRefundsVal = prevTotalRefunds != null ? prevTotalRefunds : 0.0;
        String refundsGrowth = prevPeriod[0] != null ? formatGrowthIndicator(totalRefundsVal, prevTotalRefundsVal) : "N/A";
        returnTable.addCell(new Cell().add(new Paragraph("Total Refunds")).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", totalRefundsVal))).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(refundsGrowth)).setPadding(8));
        
        // Average refund amount
        double avgRefund = totalReturnsVal > 0 ? totalRefundsVal / totalReturnsVal : 0.0;
        double prevAvgRefund = prevTotalReturnsVal > 0 ? prevTotalRefundsVal / prevTotalReturnsVal : 0.0;
        String avgRefundGrowth = prevPeriod[0] != null ? formatGrowthIndicator(avgRefund, prevAvgRefund) : "N/A";
        returnTable.addCell(new Cell().add(new Paragraph("Average Refund")).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", avgRefund))).setPadding(8));
        returnTable.addCell(new Cell().add(new Paragraph(avgRefundGrowth)).setPadding(8));
        
        document.add(returnTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addReturnDetailsSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("RETURN DETAILS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        List<ReturnOrder> returns = startDate != null && endDate != null
                ? returnOrderService.findByReturnDateBetween(startDate, endDate)
                : returnOrderService.findAllReturnOrders();
        
        if (returns.isEmpty()) {
            document.add(new Paragraph("No return orders found in the specified period.")
                    .setFontSize(10)
                    .setItalic());
        } else {
            Table returnDetailsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1}));
            returnDetailsTable.setWidth(UnitValue.createPercentValue(100));
            
            // Header row
            returnDetailsTable.addHeaderCell(new Cell().add(new Paragraph("Return ID").setBold()).setPadding(5));
            returnDetailsTable.addHeaderCell(new Cell().add(new Paragraph("Order ID").setBold()).setPadding(5));
            returnDetailsTable.addHeaderCell(new Cell().add(new Paragraph("Customer").setBold()).setPadding(5));
            returnDetailsTable.addHeaderCell(new Cell().add(new Paragraph("Refund Amount").setBold()).setPadding(5));
            returnDetailsTable.addHeaderCell(new Cell().add(new Paragraph("Status").setBold()).setPadding(5));
            
            // Data rows
            for (ReturnOrder returnOrder : returns) {
                returnDetailsTable.addCell(new Cell().add(new Paragraph(
                        String.valueOf(returnOrder.getReturnId() != null ? returnOrder.getReturnId() : returnOrder.getId()))).setPadding(5));
                returnDetailsTable.addCell(new Cell().add(new Paragraph(String.valueOf(returnOrder.getOrderId()))).setPadding(5));
                returnDetailsTable.addCell(new Cell().add(new Paragraph(
                        returnOrder.getCustomerEmail() != null ? returnOrder.getCustomerEmail() : "Guest")).setPadding(5));
                returnDetailsTable.addCell(new Cell().add(new Paragraph(
                        String.format("LKR %,.2f", returnOrder.getRefundAmount()))).setPadding(5));
                returnDetailsTable.addCell(new Cell().add(new Paragraph(returnOrder.getStatus())).setPadding(5));
            }
            
            document.add(returnDetailsTable);
        }
        
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addInventorySummarySection(Document document) {
        document.add(new Paragraph("INVENTORY SUMMARY")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        int totalProducts = productService.findAllProducts().size();
        long lowStockCount = productDetailService.findAllProductDetails().stream()
                .filter(ProductDetail::isLowStock)
                .count();
        
        Table inventoryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        inventoryTable.setWidth(UnitValue.createPercentValue(100));
        
        addSummaryRow(inventoryTable, "Total Products", String.valueOf(totalProducts));
        addSummaryRow(inventoryTable, "Low Stock Items", String.valueOf(lowStockCount));
        
        document.add(inventoryTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addLowStockSection(Document document) {
        document.add(new Paragraph("⚠️ LOW STOCK ITEMS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        List<ProductDetail> lowStockItems = productDetailService.findAllProductDetails().stream()
                .filter(ProductDetail::isLowStock)
                .limit(20) // Limit to top 20
                .toList();
        
        if (lowStockItems.isEmpty()) {
            document.add(new Paragraph("✓ No low stock items found. All products are well-stocked.")
                    .setFontSize(10)
                    .setItalic());
            document.add(new Paragraph().setMarginBottom(15));
            return;
        }
        
        Table lowStockTable = new Table(UnitValue.createPercentArray(new float[]{2, 1.5F, 1.5F, 1}));
        lowStockTable.setWidth(UnitValue.createPercentValue(100));
        
        lowStockTable.addHeaderCell(new Cell().add(new Paragraph("Product").setBold()).setPadding(5));
        lowStockTable.addHeaderCell(new Cell().add(new Paragraph("Current Stock").setBold()).setPadding(5));
        lowStockTable.addHeaderCell(new Cell().add(new Paragraph("Threshold").setBold()).setPadding(5));
        lowStockTable.addHeaderCell(new Cell().add(new Paragraph("Status").setBold()).setPadding(5));
        
        for (ProductDetail detail : lowStockItems) {
            // Get product name using productCode
            String productName = "Unknown";
            try {
                Product product = productService.findProduct(detail.getProductCode());
                if (product != null && product.getDescription() != null) {
                    productName = product.getDescription();
                }
            } catch (Exception e) {
                // If product not found, use default name
                productName = "Product #" + detail.getProductCode();
            }
            
            int currentStock = detail.getQtyOnHand();
            int threshold = detail.getLowStockThreshold() != null ? detail.getLowStockThreshold() : 10;
            String status = currentStock == 0 ? "OUT OF STOCK" : "LOW STOCK";
            
            lowStockTable.addCell(new Cell().add(new Paragraph(productName)).setPadding(5));
            lowStockTable.addCell(new Cell().add(new Paragraph(String.valueOf(currentStock))).setPadding(5));
            lowStockTable.addCell(new Cell().add(new Paragraph(String.valueOf(threshold))).setPadding(5));
            lowStockTable.addCell(new Cell().add(new Paragraph(status)).setPadding(5));
        }
        
        document.add(lowStockTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addProductDetailsSection(Document document) {
        document.add(new Paragraph("PRODUCT DETAILS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        // Implementation would list all products with details
        document.add(new Paragraph("Product details would be displayed here.")
                .setFontSize(10)
                .setItalic());
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addFinancialSummarySection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("FINANCIAL SUMMARY")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        Double revenue = startDate != null && endDate != null
                ? orderDetailService.getRevenueByDateRange(startDate, endDate)
                : orderDetailService.getTotalRevenue();
        Double refunds = startDate != null && endDate != null
                ? returnOrderService.getTotalRefundAmountByDateRange(startDate, endDate)
                : returnOrderService.getTotalRefundAmount();
        Double netRevenue = (revenue != null ? revenue : 0.0) - (refunds != null ? refunds : 0.0);
        
        Table financialTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        financialTable.setWidth(UnitValue.createPercentValue(100));
        
        addSummaryRow(financialTable, "Gross Revenue", String.format("LKR %,.2f", revenue != null ? revenue : 0.0));
        addSummaryRow(financialTable, "Total Refunds", String.format("LKR %,.2f", refunds != null ? refunds : 0.0));
        addSummaryRow(financialTable, "Net Revenue", String.format("LKR %,.2f", netRevenue));
        
        document.add(financialTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addRevenueVsReturnsSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("📊 REVENUE VS RETURNS ANALYSIS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        Double revenue = startDate != null && endDate != null
                ? orderDetailService.getRevenueByDateRange(startDate, endDate)
                : orderDetailService.getTotalRevenue();
        Double refunds = startDate != null && endDate != null
                ? returnOrderService.getTotalRefundAmountByDateRange(startDate, endDate)
                : returnOrderService.getTotalRefundAmount();
        Long orders = startDate != null && endDate != null
                ? orderDetailService.countOrdersByDateRange(startDate, endDate)
                : orderDetailService.getTotalOrderCount();
        Long returns = startDate != null && endDate != null
                ? returnOrderService.countReturnsByDateRange(startDate, endDate)
                : (long) returnOrderService.findAllReturnOrders().size();
        
        double revenueVal = revenue != null ? revenue : 0.0;
        double refundsVal = refunds != null ? refunds : 0.0;
        long ordersVal = orders != null ? orders : 0;
        long returnsVal = returns != null ? returns : 0;
        
        double netRevenue = revenueVal - refundsVal;
        double returnRate = ordersVal > 0 ? (returnsVal * 100.0 / ordersVal) : 0.0;
        double refundRate = revenueVal > 0 ? (refundsVal * 100.0 / revenueVal) : 0.0;
        
        Table comparisonTable = new Table(UnitValue.createPercentArray(new float[]{2, 2}));
        comparisonTable.setWidth(UnitValue.createPercentValue(100));
        
        comparisonTable.addHeaderCell(new Cell().add(new Paragraph("Metric").setBold()).setPadding(8));
        comparisonTable.addHeaderCell(new Cell().add(new Paragraph("Value").setBold()).setPadding(8));
        
        addSummaryRow(comparisonTable, "💰 Gross Revenue", String.format("LKR %,.2f", revenueVal));
        addSummaryRow(comparisonTable, "💸 Total Refunds", String.format("LKR %,.2f", refundsVal));
        addSummaryRow(comparisonTable, "💵 Net Revenue", String.format("LKR %,.2f", netRevenue));
        addSummaryRow(comparisonTable, "📦 Total Orders", String.valueOf(ordersVal));
        addSummaryRow(comparisonTable, "🔄 Total Returns", String.valueOf(returnsVal));
        addSummaryRow(comparisonTable, "📉 Return Rate", String.format("%.2f%%", returnRate));
        addSummaryRow(comparisonTable, "📊 Refund Rate", String.format("%.2f%%", refundRate));
        
        // Health indicator
        String healthStatus;
        if (returnRate < 5.0 && refundRate < 2.0) {
            healthStatus = "✓ Excellent - Low return and refund rates";
        } else if (returnRate < 10.0 && refundRate < 5.0) {
            healthStatus = "✓ Good - Acceptable return and refund rates";
        } else if (returnRate < 15.0 && refundRate < 10.0) {
            healthStatus = "⚠️ Fair - Monitor return and refund trends";
        } else {
            healthStatus = "⚠️ Needs Attention - High return/refund rates";
        }
        
        document.add(comparisonTable);
        document.add(new Paragraph().setMarginTop(10));
        Paragraph healthIndicator = new Paragraph("📌 Business Health: " + healthStatus)
                .setFontSize(11)
                .setBold()
                .setMarginBottom(15);
        document.add(healthIndicator);
    }
    
    private void addTopCustomersSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("👥 TOP CUSTOMERS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        List<Object[]> topCustomers;
        if (startDate != null && endDate != null) {
            // Filter top customers by date range
            List<OrderDetail> orders = orderDetailService.findOrdersByDateRange(startDate, endDate);
            Map<String, Double> customerRevenue = orders.stream()
                    .filter(o -> o.getCustomerName() != null && "PAID".equals(o.getPaymentStatus()))
                    .collect(Collectors.groupingBy(
                        OrderDetail::getCustomerName,
                        Collectors.summingDouble(OrderDetail::getTotalCost)
                    ));
            
            topCustomers = customerRevenue.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(10)
                    .map(e -> new Object[]{e.getKey(), e.getValue()})
                    .collect(Collectors.toList());
        } else {
            topCustomers = orderDetailService.getTopCustomersByRevenue();
        }
        
        if (topCustomers.isEmpty()) {
            document.add(new Paragraph("No customer data available.")
                    .setFontSize(10)
                    .setItalic());
            document.add(new Paragraph().setMarginBottom(15));
            return;
        }
        
        // Limit to top 10
        topCustomers = topCustomers.stream().limit(10).collect(Collectors.toList());
        
        Table customersTable = new Table(UnitValue.createPercentArray(new float[]{0.5F, 2.5F, 2}));
        customersTable.setWidth(UnitValue.createPercentValue(100));
        
        customersTable.addHeaderCell(new Cell().add(new Paragraph("#").setBold()).setPadding(5));
        customersTable.addHeaderCell(new Cell().add(new Paragraph("Customer Name").setBold()).setPadding(5));
        customersTable.addHeaderCell(new Cell().add(new Paragraph("Total Spent").setBold()).setPadding(5));
        
        int rank = 1;
        for (Object[] customer : topCustomers) {
            String customerName = (String) customer[0];
            Double totalSpent = ((Number) customer[1]).doubleValue();
            
            customersTable.addCell(new Cell().add(new Paragraph(String.valueOf(rank++))).setPadding(5));
            customersTable.addCell(new Cell().add(new Paragraph(customerName != null ? customerName : "Guest")).setPadding(5));
            customersTable.addCell(new Cell().add(new Paragraph(String.format("LKR %,.2f", totalSpent))).setPadding(5));
        }
        
        document.add(customersTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addSupplierSummarySection(Document document) {
        document.add(new Paragraph("SUPPLIER SUMMARY")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        int totalSuppliers = supplierService.findAllSuppliers().size();
        
        Table supplierTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        supplierTable.setWidth(UnitValue.createPercentValue(100));
        
        addSummaryRow(supplierTable, "Total Suppliers", String.valueOf(totalSuppliers));
        
        document.add(supplierTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addPurchaseOrdersSection(Document document) {
        document.add(new Paragraph("PURCHASE ORDERS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        // Implementation would load purchase orders
        document.add(new Paragraph("Purchase orders data would be displayed here.")
                .setFontSize(10)
                .setItalic());
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addExecutiveSummarySection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("EXECUTIVE SUMMARY")
                .setFontSize(18)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        // Comprehensive summary of all key metrics
        addSummarySection(document, startDate, endDate);
        addReturnStatisticsSection(document, startDate, endDate);
        addInventorySummarySection(document);
    }
    
    private void addSalesAnalysisSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("SALES ANALYSIS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        addSalesByPeriodSection(document, startDate, endDate, "comprehensive");
        addTopProductsSection(document, startDate, endDate);
        addSalesByCategorySection(document, startDate, endDate);
    }
    
    private void addReturnOrdersAnalysisSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("RETURN ORDERS ANALYSIS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        addReturnStatisticsSection(document, startDate, endDate);
        addReturnDetailsSection(document, startDate, endDate);
    }
    
    private void addInventoryStatusSection(Document document) {
        document.add(new Paragraph("INVENTORY STATUS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        addInventorySummarySection(document);
        addLowStockSection(document);
    }
    
    private void addCustomerAnalysisSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("CUSTOMER ANALYSIS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        addTopCustomersSection(document, startDate, endDate);
    }
    
    private void addSupplierAnalysisSection(Document document) {
        document.add(new Paragraph("SUPPLIER ANALYSIS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        addSupplierSummarySection(document);
        addPurchaseOrdersSection(document);
    }
    
    private void addFooter(Document document) {
        document.add(new Paragraph().setMarginTop(20));
        
        // Get system settings for footer
        SystemSettings settings = systemSettingsService.getSystemSettings();
        String businessName = settings.getBusinessName() != null && !settings.getBusinessName().trim().isEmpty()
            ? settings.getBusinessName()
            : "Lilarathne POS System";
        
        String footerMessage = settings.getFooterMessage() != null && !settings.getFooterMessage().trim().isEmpty()
            ? settings.getFooterMessage()
            : "Thank you for your business!";
        
        Paragraph footer = new Paragraph(
                "This report was generated by " + businessName + "\n" +
                footerMessage + "\n" +
                "For questions or support, please contact your system administrator.")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic();
        document.add(footer);
    }
}

