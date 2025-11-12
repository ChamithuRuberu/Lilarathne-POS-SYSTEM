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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PDFReportService {
    
    private final OrderDetailService orderDetailService;
    private final ReturnOrderService returnOrderService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final SupplierService supplierService;
    private final ProductDetailService productDetailService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
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
        Paragraph header = new Paragraph(title)
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(header);
        
        if (startDate != null && endDate != null) {
            Paragraph dateRange = new Paragraph(
                    "Period: " + startDate.format(DATE_ONLY_FORMATTER) + " to " + endDate.format(DATE_ONLY_FORMATTER))
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
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
    
    private void addSummarySection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("SUMMARY STATISTICS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
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
        
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        summaryTable.setWidth(UnitValue.createPercentValue(100));
        
        addSummaryRow(summaryTable, "Total Revenue", String.format("LKR %,.2f", revenue != null ? revenue : 0.0));
        addSummaryRow(summaryTable, "Total Refunds", String.format("LKR %,.2f", refunds != null ? refunds : 0.0));
        addSummaryRow(summaryTable, "Net Revenue", String.format("LKR %,.2f", netRevenue));
        addSummaryRow(summaryTable, "Total Orders", String.valueOf(orders != null ? orders : 0));
        addSummaryRow(summaryTable, "Total Returns", String.valueOf(returns != null ? returns : 0));
        addSummaryRow(summaryTable, "Average Order Value", String.format("LKR %,.2f", avgOrder != null ? avgOrder : 0.0));
        
        document.add(summaryTable);
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addSummaryRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()).setPadding(5));
        table.addCell(new Cell().add(new Paragraph(value)).setPadding(5));
    }
    
    private void addSalesByPeriodSection(Document document, LocalDateTime startDate, LocalDateTime endDate, String reportType) {
        document.add(new Paragraph("SALES BY PERIOD")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        // Implementation would load sales data by period
        // This is a placeholder - actual implementation would query the database
        document.add(new Paragraph("Sales period data would be displayed here based on " + reportType + " view.")
                .setFontSize(10)
                .setItalic());
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addTopProductsSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("TOP SELLING PRODUCTS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        // Implementation would load top products
        document.add(new Paragraph("Top products data would be displayed here.")
                .setFontSize(10)
                .setItalic());
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addSalesByCategorySection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("SALES BY CATEGORY")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        // Implementation would load sales by category
        document.add(new Paragraph("Category sales data would be displayed here.")
                .setFontSize(10)
                .setItalic());
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addReturnStatisticsSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("RETURN STATISTICS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        Long totalReturns = startDate != null && endDate != null
                ? returnOrderService.countReturnsByDateRange(startDate, endDate)
                : (long) returnOrderService.findAllReturnOrders().size();
        Long pendingReturns = returnOrderService.countByStatus("PENDING");
        Double totalRefunds = startDate != null && endDate != null
                ? returnOrderService.getTotalRefundAmountByDateRange(startDate, endDate)
                : returnOrderService.getTotalRefundAmount();
        
        Table returnTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        returnTable.setWidth(UnitValue.createPercentValue(100));
        
        addSummaryRow(returnTable, "Total Returns", String.valueOf(totalReturns != null ? totalReturns : 0));
        addSummaryRow(returnTable, "Pending Returns", String.valueOf(pendingReturns != null ? pendingReturns : 0));
        addSummaryRow(returnTable, "Total Refunds", String.format("LKR %,.2f", totalRefunds != null ? totalRefunds : 0.0));
        
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
        document.add(new Paragraph("LOW STOCK ITEMS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        List<ProductDetail> lowStockItems = productDetailService.findAllProductDetails().stream()
                .filter(ProductDetail::isLowStock)
                .toList();
        
        if (lowStockItems.isEmpty()) {
            document.add(new Paragraph("No low stock items found.")
                    .setFontSize(10)
                    .setItalic());
        } else {
            // Implementation would create a table with low stock items
            document.add(new Paragraph("Low stock items would be listed here.")
                    .setFontSize(10)
                    .setItalic());
        }
        
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
        document.add(new Paragraph("REVENUE VS RETURNS ANALYSIS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        // Implementation would show comparison
        document.add(new Paragraph("Revenue vs returns comparison would be displayed here.")
                .setFontSize(10)
                .setItalic());
        document.add(new Paragraph().setMarginBottom(15));
    }
    
    private void addTopCustomersSection(Document document, LocalDateTime startDate, LocalDateTime endDate) {
        document.add(new Paragraph("TOP CUSTOMERS")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(10));
        
        // Implementation would load top customers
        document.add(new Paragraph("Top customers data would be displayed here.")
                .setFontSize(10)
                .setItalic());
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
        Paragraph footer = new Paragraph(
                "This report was generated by Lilarathne POS System\n" +
                "For questions or support, please contact your system administrator.")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic();
        document.add(footer);
    }
}

