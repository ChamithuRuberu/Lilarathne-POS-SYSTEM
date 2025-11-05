package com.devstack.pos.controller;

import com.devstack.pos.entity.Product;
import com.devstack.pos.repository.ProductDetailRepository;
import com.devstack.pos.repository.ProductRepository;
import com.devstack.pos.service.ItemDetailService;
import com.devstack.pos.service.OrderDetailService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AnalysisPageController extends BaseController {
    
    // Date filters
    @FXML
    private DatePicker dateFrom;
    
    @FXML
    private DatePicker dateTo;
    
    // Summary statistics
    @FXML
    private Text lblTotalRevenue;
    
    @FXML
    private Text lblTotalOrders;
    
    @FXML
    private Text lblTotalProfit;
    
    @FXML
    private Text lblAvgOrderValue;
    
    // Sales Reports Tab
    @FXML
    private TableView<SalesReportTm> tblSalesReports;
    
    @FXML
    private TableColumn<SalesReportTm, String> colSalesPeriod;
    
    @FXML
    private TableColumn<SalesReportTm, Integer> colSalesOrders;
    
    @FXML
    private TableColumn<SalesReportTm, Double> colSalesRevenue;
    
    @FXML
    private TableColumn<SalesReportTm, Double> colSalesProfit;
    
    // Top Products Tab
    @FXML
    private TableView<TopProductTm> tblTopProducts;
    
    @FXML
    private TableColumn<TopProductTm, Integer> colProductRank;
    
    @FXML
    private TableColumn<TopProductTm, String> colProductName;
    
    @FXML
    private TableColumn<TopProductTm, Integer> colProductQtySold;
    
    @FXML
    private TableColumn<TopProductTm, Double> colProductRevenue;
    
    // Sales by Category Tab
    @FXML
    private TableView<CategoryReportTm> tblSalesByCategory;
    
    @FXML
    private TableColumn<CategoryReportTm, String> colCategoryName;
    
    @FXML
    private TableColumn<CategoryReportTm, Integer> colCategoryOrders;
    
    @FXML
    private TableColumn<CategoryReportTm, Double> colCategoryRevenue;
    
    @FXML
    private TableColumn<CategoryReportTm, Double> colCategoryProfit;
    
    // Sales by Cashier Tab
    @FXML
    private TableView<CashierReportTm> tblSalesByCashier;
    
    @FXML
    private TableColumn<CashierReportTm, Integer> colCashierRank;
    
    @FXML
    private TableColumn<CashierReportTm, String> colCashierName;
    
    @FXML
    private TableColumn<CashierReportTm, Integer> colCashierOrders;
    
    @FXML
    private TableColumn<CashierReportTm, Double> colCashierRevenue;
    
    // Profit & Loss Tab
    @FXML
    private Text lblPLTotalSales;
    
    @FXML
    private Text lblPLCostOfGoods;
    
    @FXML
    private Text lblPLGrossProfit;
    
    @FXML
    private Text lblPLProfitMargin;
    
    @FXML
    private TableView<ProfitLossTm> tblProfitLoss;
    
    @FXML
    private TableColumn<ProfitLossTm, String> colPLProduct;
    
    @FXML
    private TableColumn<ProfitLossTm, Integer> colPLQtySold;
    
    @FXML
    private TableColumn<ProfitLossTm, Double> colPLCost;
    
    @FXML
    private TableColumn<ProfitLossTm, Double> colPLRevenue;
    
    @FXML
    private TableColumn<ProfitLossTm, Double> colPLProfit;
    
    // Tax Summary Tab
    @FXML
    private Text lblTaxableAmount;
    
    @FXML
    private Text lblEstimatedTax;
    
    @FXML
    private TableView<TaxSummaryTm> tblTaxSummary;
    
    @FXML
    private TableColumn<TaxSummaryTm, String> colTaxPeriod;
    
    @FXML
    private TableColumn<TaxSummaryTm, Integer> colTaxOrders;
    
    @FXML
    private TableColumn<TaxSummaryTm, Double> colTaxGrossSales;
    
    @FXML
    private TableColumn<TaxSummaryTm, Double> colTaxAmount;
    
    // Top Customers Tab
    @FXML
    private TableView<TopCustomerTm> tblTopCustomers;
    
    @FXML
    private TableColumn<TopCustomerTm, Integer> colCustomerRank;
    
    @FXML
    private TableColumn<TopCustomerTm, String> colCustomerEmail;
    
    @FXML
    private TableColumn<TopCustomerTm, Integer> colCustomerOrders;
    
    @FXML
    private TableColumn<TopCustomerTm, Double> colCustomerRevenue;
    
    private final OrderDetailService orderDetailService;
    private final ItemDetailService itemDetailService;
    private final ProductRepository productRepository;
    private final ProductDetailRepository productDetailRepository;
    
    private LocalDateTime filterStartDate = null;
    private LocalDateTime filterEndDate = null;
    
    @FXML
    public void initialize() {
        // Initialize sidebar
        initializeSidebar();
        
        // Initialize date pickers
        dateFrom.setValue(LocalDate.now().minusMonths(1));
        dateTo.setValue(LocalDate.now());
        
        // Configure Sales Reports Table
        colSalesPeriod.setCellValueFactory(new PropertyValueFactory<>("period"));
        colSalesOrders.setCellValueFactory(new PropertyValueFactory<>("orders"));
        colSalesRevenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        colSalesProfit.setCellValueFactory(new PropertyValueFactory<>("profit"));
        
        // Configure Top Products Table
        colProductRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colProductQtySold.setCellValueFactory(new PropertyValueFactory<>("qtySold"));
        colProductRevenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        
        // Configure Sales by Category Table
        colCategoryName.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colCategoryOrders.setCellValueFactory(new PropertyValueFactory<>("orders"));
        colCategoryRevenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        colCategoryProfit.setCellValueFactory(new PropertyValueFactory<>("profit"));
        
        // Configure Sales by Cashier Table
        colCashierRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colCashierName.setCellValueFactory(new PropertyValueFactory<>("cashierName"));
        colCashierOrders.setCellValueFactory(new PropertyValueFactory<>("orders"));
        colCashierRevenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        
        // Configure Profit & Loss Table
        colPLProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colPLQtySold.setCellValueFactory(new PropertyValueFactory<>("qtySold"));
        colPLCost.setCellValueFactory(new PropertyValueFactory<>("cost"));
        colPLRevenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        colPLProfit.setCellValueFactory(new PropertyValueFactory<>("profit"));
        
        // Configure Tax Summary Table
        colTaxPeriod.setCellValueFactory(new PropertyValueFactory<>("period"));
        colTaxOrders.setCellValueFactory(new PropertyValueFactory<>("orders"));
        colTaxGrossSales.setCellValueFactory(new PropertyValueFactory<>("grossSales"));
        colTaxAmount.setCellValueFactory(new PropertyValueFactory<>("taxAmount"));
        
        // Configure Top Customers Table
        colCustomerRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colCustomerEmail.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colCustomerOrders.setCellValueFactory(new PropertyValueFactory<>("orders"));
        colCustomerRevenue.setCellValueFactory(new PropertyValueFactory<>("totalRevenue"));
        
        // Load all data
        loadAllReports();
    }
    
    @FXML
    public void refreshAll(ActionEvent event) {
        loadAllReports();
    }


    @Override
    protected String getCurrentPageName() {
        return "Reports & Analytics";
    }
    
    public void btnBackToDashboard(ActionEvent actionEvent) {
        btnDashboardOnAction(actionEvent);
    }

    // Navigation methods inherited from BaseController

    @FXML
    public void applyDateFilter(ActionEvent event) {
        if (dateFrom.getValue() != null && dateTo.getValue() != null) {
            filterStartDate = dateFrom.getValue().atStartOfDay();
            filterEndDate = dateTo.getValue().atTime(23, 59, 59);
            loadAllReports();
        }
    }
    
    private void loadAllReports() {
        loadSummaryStatistics();
        loadTopProducts();
        loadSalesByCategory();
        loadSalesByCashier();
        loadProfitLoss();
        loadTaxSummary();
        loadTopCustomers();
    }
    
    private void loadSummaryStatistics() {
        Double revenue;
        Long orders;
        Double profit;
        Double avgOrder;
        
        if (filterStartDate != null && filterEndDate != null) {
            revenue = orderDetailService.getRevenueByDateRange(filterStartDate, filterEndDate);
            orders = orderDetailService.countOrdersByDateRange(filterStartDate, filterEndDate);
            profit = itemDetailService.getProfitByDateRange(filterStartDate, filterEndDate);
            avgOrder = orderDetailService.getAverageOrderValueByDateRange(filterStartDate, filterEndDate);
        } else {
            revenue = orderDetailService.getTotalRevenue();
            orders = orderDetailService.getTotalOrderCount();
            profit = itemDetailService.getTotalProfit();
            avgOrder = orderDetailService.getAverageOrderValue();
        }
        
        lblTotalRevenue.setText(String.format("%.2f /=", revenue != null ? revenue : 0.0));
        lblTotalOrders.setText(String.valueOf(orders != null ? orders : 0));
        lblTotalProfit.setText(String.format("%.2f /=", profit != null ? profit : 0.0));
        lblAvgOrderValue.setText(String.format("%.2f /=", avgOrder != null ? avgOrder : 0.0));
    }
    
    @FXML
    public void loadWeeklySales(ActionEvent event) {
        // Load last 8 weeks
        ObservableList<SalesReportTm> data = FXCollections.observableArrayList();
        LocalDate now = LocalDate.now();
        
        for (int i = 7; i >= 0; i--) {
            LocalDate weekStart = now.minusWeeks(i).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);
            
            LocalDateTime startDateTime = weekStart.atStartOfDay();
            LocalDateTime endDateTime = weekEnd.atTime(23, 59, 59);
            
            Double revenue = orderDetailService.getRevenueByDateRange(startDateTime, endDateTime);
            Long orders = orderDetailService.countOrdersByDateRange(startDateTime, endDateTime);
            Double profit = itemDetailService.getProfitByDateRange(startDateTime, endDateTime);
            
            String period = weekStart.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " + 
                          weekEnd.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            
            data.add(new SalesReportTm(period, orders.intValue(), revenue, profit));
        }
        
        tblSalesReports.setItems(data);
    }
    
    @FXML
    public void loadMonthlySales(ActionEvent event) {
        // Load last 12 months
        ObservableList<SalesReportTm> data = FXCollections.observableArrayList();
        LocalDate now = LocalDate.now();
        
        for (int i = 11; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).with(TemporalAdjusters.firstDayOfMonth());
            LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());
            
            LocalDateTime startDateTime = monthStart.atStartOfDay();
            LocalDateTime endDateTime = monthEnd.atTime(23, 59, 59);
            
            Double revenue = orderDetailService.getRevenueByDateRange(startDateTime, endDateTime);
            Long orders = orderDetailService.countOrdersByDateRange(startDateTime, endDateTime);
            Double profit = itemDetailService.getProfitByDateRange(startDateTime, endDateTime);
            
            String period = monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            
            data.add(new SalesReportTm(period, orders.intValue(), revenue, profit));
        }
        
        tblSalesReports.setItems(data);
    }
    
    @FXML
    public void loadYearlySales(ActionEvent event) {
        // Load last 5 years
        ObservableList<SalesReportTm> data = FXCollections.observableArrayList();
        LocalDate now = LocalDate.now();
        
        for (int i = 4; i >= 0; i--) {
            LocalDate yearStart = now.minusYears(i).with(TemporalAdjusters.firstDayOfYear());
            LocalDate yearEnd = yearStart.with(TemporalAdjusters.lastDayOfYear());
            
            LocalDateTime startDateTime = yearStart.atStartOfDay();
            LocalDateTime endDateTime = yearEnd.atTime(23, 59, 59);
            
            Double revenue = orderDetailService.getRevenueByDateRange(startDateTime, endDateTime);
            Long orders = orderDetailService.countOrdersByDateRange(startDateTime, endDateTime);
            Double profit = itemDetailService.getProfitByDateRange(startDateTime, endDateTime);
            
            String period = String.valueOf(yearStart.getYear());
            
            data.add(new SalesReportTm(period, orders.intValue(), revenue, profit));
        }
        
        tblSalesReports.setItems(data);
    }
    
    private void loadTopProducts() {
        List<Object[]> topProductsData = itemDetailService.getTopSellingProductsByRevenue();
        ObservableList<TopProductTm> observableList = FXCollections.observableArrayList();
        
        // Create a map to lookup product names
        Map<String, String> productNames = new HashMap<>();
        List<Product> allProducts = productRepository.findAll();
        for (Product product : allProducts) {
            productNames.put(String.valueOf(product.getCode()), product.getDescription());
        }
        
        int rank = 1;
        for (Object[] data : topProductsData) {
            if (rank > 10) break; // Top 10 only
            
            String detailCode = (String) data[0];
            Integer qtySold = ((Number) data[1]).intValue();
            Double revenue = ((Number) data[2]).doubleValue();
            
            // Get product name from detail code
            String productName = productNames.getOrDefault(detailCode, "Product #" + detailCode);
            
            TopProductTm tm = new TopProductTm(rank++, productName, qtySold, revenue);
            observableList.add(tm);
        }
        
        tblTopProducts.setItems(observableList);
    }
    
    private void loadSalesByCategory() {
        List<Object[]> categoryData = itemDetailService.getSalesByCategory();
        ObservableList<CategoryReportTm> observableList = FXCollections.observableArrayList();
        
        // Create a map to lookup product names
        Map<Integer, String> productNames = new HashMap<>();
        List<Product> allProducts = productRepository.findAll();
        for (Product product : allProducts) {
            productNames.put(product.getCode(), product.getDescription());
        }
        
        for (Object[] data : categoryData) {
            Integer productCode = (Integer) data[0];
            Integer qtySold = ((Number) data[1]).intValue();
            Double revenue = ((Number) data[2]).doubleValue();
            
            String categoryName = productNames.getOrDefault(productCode, "Category #" + productCode);
            
            // Estimate profit (would need to calculate from buying/selling prices)
            Double profit = revenue * 0.25; // Assuming 25% margin as estimate
            
            CategoryReportTm tm = new CategoryReportTm(categoryName, qtySold, revenue, profit);
            observableList.add(tm);
        }
        
        tblSalesByCategory.setItems(observableList);
    }
    
    private void loadSalesByCashier() {
        List<Object[]> cashierData;
        
        if (filterStartDate != null && filterEndDate != null) {
            cashierData = orderDetailService.getSalesByCashierByDateRange(filterStartDate, filterEndDate);
        } else {
            cashierData = orderDetailService.getSalesByCashier();
        }
        
        ObservableList<CashierReportTm> observableList = FXCollections.observableArrayList();
        
        int rank = 1;
        for (Object[] data : cashierData) {
            String cashierEmail = (String) data[0];
            Integer orders = ((Number) data[1]).intValue();
            Double revenue = ((Number) data[2]).doubleValue();
            
            CashierReportTm tm = new CashierReportTm(rank++, cashierEmail != null ? cashierEmail : "Unknown", orders, revenue);
            observableList.add(tm);
        }
        
        tblSalesByCashier.setItems(observableList);
    }
    
    private void loadProfitLoss() {
        Double totalSales;
        Double costOfGoods;
        Double grossProfit;
        
        if (filterStartDate != null && filterEndDate != null) {
            totalSales = orderDetailService.getRevenueByDateRange(filterStartDate, filterEndDate);
            costOfGoods = itemDetailService.getCostOfGoodsByDateRange(filterStartDate, filterEndDate);
            grossProfit = itemDetailService.getProfitByDateRange(filterStartDate, filterEndDate);
        } else {
            totalSales = orderDetailService.getTotalRevenue();
            costOfGoods = itemDetailService.getCostOfGoodsSold();
            grossProfit = itemDetailService.getTotalProfit();
        }
        
        lblPLTotalSales.setText(String.format("%.2f /=", totalSales != null ? totalSales : 0.0));
        lblPLCostOfGoods.setText(String.format("%.2f /=", costOfGoods != null ? costOfGoods : 0.0));
        lblPLGrossProfit.setText(String.format("%.2f /=", grossProfit != null ? grossProfit : 0.0));
        
        double profitMargin = (totalSales != null && totalSales > 0) ? (grossProfit / totalSales * 100) : 0.0;
        lblPLProfitMargin.setText(String.format("%.2f%%", profitMargin));
        
        // Load detailed profit by product
        List<Object[]> profitData = itemDetailService.getProfitByProduct();
        ObservableList<ProfitLossTm> observableList = FXCollections.observableArrayList();
        
        for (Object[] data : profitData) {
            String productCode = (String) data[0];
            String productName = (String) data[1];
            Integer qtySold = ((Number) data[2]).intValue();
            Double cost = ((Number) data[3]).doubleValue();
            Double revenue = ((Number) data[4]).doubleValue();
            Double profit = ((Number) data[5]).doubleValue();
            
            ProfitLossTm tm = new ProfitLossTm(productName, qtySold, cost, revenue, profit);
            observableList.add(tm);
        }
        
        tblProfitLoss.setItems(observableList);
    }
    
    private void loadTaxSummary() {
        Double taxableAmount;
        
        if (filterStartDate != null && filterEndDate != null) {
            taxableAmount = orderDetailService.getRevenueByDateRange(filterStartDate, filterEndDate);
        } else {
            taxableAmount = orderDetailService.getTotalRevenue();
        }
        
        double tax = (taxableAmount != null ? taxableAmount : 0.0) * 0.15; // 15% tax
        
        lblTaxableAmount.setText(String.format("%.2f /=", taxableAmount != null ? taxableAmount : 0.0));
        lblEstimatedTax.setText(String.format("%.2f /=", tax));
        
        // Load monthly tax summary for last 12 months
        ObservableList<TaxSummaryTm> data = FXCollections.observableArrayList();
        LocalDate now = LocalDate.now();
        
        for (int i = 11; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).with(TemporalAdjusters.firstDayOfMonth());
            LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());
            
            LocalDateTime startDateTime = monthStart.atStartOfDay();
            LocalDateTime endDateTime = monthEnd.atTime(23, 59, 59);
            
            Double revenue = orderDetailService.getRevenueByDateRange(startDateTime, endDateTime);
            Long orders = orderDetailService.countOrdersByDateRange(startDateTime, endDateTime);
            Double taxAmount = (revenue != null ? revenue : 0.0) * 0.15;
            
            String period = monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            
            data.add(new TaxSummaryTm(period, orders.intValue(), revenue, taxAmount));
        }
        
        tblTaxSummary.setItems(data);
    }
    
    private void loadTopCustomers() {
        List<Object[]> topCustomersData;
        
        if (filterStartDate != null && filterEndDate != null) {
            topCustomersData = orderDetailService.getTopCustomersWithOrderCountByDateRange(filterStartDate, filterEndDate);
        } else {
            topCustomersData = orderDetailService.getTopCustomersWithOrderCount();
        }
        
        ObservableList<TopCustomerTm> observableList = FXCollections.observableArrayList();
        
        int rank = 1;
        for (Object[] data : topCustomersData) {
            String customerName = (String) data[0];
            Integer orders = ((Number) data[1]).intValue();
            Double totalRevenue = ((Number) data[2]).doubleValue();
            
            TopCustomerTm tm = new TopCustomerTm(rank++, customerName != null ? customerName : "Guest", orders, totalRevenue);
            observableList.add(tm);
        }
        
        tblTopCustomers.setItems(observableList);
    }
    
    // ===== Table Model Classes =====
    
    public static class SalesReportTm {
        private String period;
        private int orders;
        private double revenue;
        private double profit;
        
        public SalesReportTm(String period, int orders, double revenue, double profit) {
            this.period = period;
            this.orders = orders;
            this.revenue = revenue;
            this.profit = profit;
        }
        
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public int getOrders() { return orders; }
        public void setOrders(int orders) { this.orders = orders; }
        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
        public double getProfit() { return profit; }
        public void setProfit(double profit) { this.profit = profit; }
    }
    
    public static class TopProductTm {
        private int rank;
        private String productName;
        private int qtySold;
        private double revenue;
        
        public TopProductTm(int rank, String productName, int qtySold, double revenue) {
            this.rank = rank;
            this.productName = productName;
            this.qtySold = qtySold;
            this.revenue = revenue;
        }
        
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getQtySold() { return qtySold; }
        public void setQtySold(int qtySold) { this.qtySold = qtySold; }
        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
    }
    
    public static class CategoryReportTm {
        private String categoryName;
        private int orders;
        private double revenue;
        private double profit;
        
        public CategoryReportTm(String categoryName, int orders, double revenue, double profit) {
            this.categoryName = categoryName;
            this.orders = orders;
            this.revenue = revenue;
            this.profit = profit;
        }
        
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public int getOrders() { return orders; }
        public void setOrders(int orders) { this.orders = orders; }
        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
        public double getProfit() { return profit; }
        public void setProfit(double profit) { this.profit = profit; }
    }
    
    public static class CashierReportTm {
        private int rank;
        private String cashierName;
        private int orders;
        private double revenue;
        
        public CashierReportTm(int rank, String cashierName, int orders, double revenue) {
            this.rank = rank;
            this.cashierName = cashierName;
            this.orders = orders;
            this.revenue = revenue;
        }
        
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public String getCashierName() { return cashierName; }
        public void setCashierName(String cashierName) { this.cashierName = cashierName; }
        public int getOrders() { return orders; }
        public void setOrders(int orders) { this.orders = orders; }
        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
    }
    
    public static class ProfitLossTm {
        private String productName;
        private int qtySold;
        private double cost;
        private double revenue;
        private double profit;
        
        public ProfitLossTm(String productName, int qtySold, double cost, double revenue, double profit) {
            this.productName = productName;
            this.qtySold = qtySold;
            this.cost = cost;
            this.revenue = revenue;
            this.profit = profit;
        }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getQtySold() { return qtySold; }
        public void setQtySold(int qtySold) { this.qtySold = qtySold; }
        public double getCost() { return cost; }
        public void setCost(double cost) { this.cost = cost; }
        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
        public double getProfit() { return profit; }
        public void setProfit(double profit) { this.profit = profit; }
    }
    
    public static class TaxSummaryTm {
        private String period;
        private int orders;
        private double grossSales;
        private double taxAmount;
        
        public TaxSummaryTm(String period, int orders, double grossSales, double taxAmount) {
            this.period = period;
            this.orders = orders;
            this.grossSales = grossSales;
            this.taxAmount = taxAmount;
        }
        
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public int getOrders() { return orders; }
        public void setOrders(int orders) { this.orders = orders; }
        public double getGrossSales() { return grossSales; }
        public void setGrossSales(double grossSales) { this.grossSales = grossSales; }
        public double getTaxAmount() { return taxAmount; }
        public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }
    }
    
    public static class TopCustomerTm {
        private int rank;
        private String customerName;
        private int orders;
        private double totalRevenue;
        
        public TopCustomerTm(int rank, String customerName, int orders, double totalRevenue) {
            this.rank = rank;
            this.customerName = customerName;
            this.orders = orders;
            this.totalRevenue = totalRevenue;
        }
        
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public int getOrders() { return orders; }
        public void setOrders(int orders) { this.orders = orders; }
        public double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
    }
}
