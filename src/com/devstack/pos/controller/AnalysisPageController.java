package com.devstack.pos.controller;

import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.Product;
import com.devstack.pos.repository.ProductDetailRepository;
import com.devstack.pos.repository.ProductRepository;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.OrderItemService;
import com.devstack.pos.service.PDFReportService;
import com.devstack.pos.service.ReturnOrderItemService;
import com.devstack.pos.service.ReturnOrderService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    private TableColumn<SalesReportTm, Double> colSalesRefunds;
    
    @FXML
    private TableColumn<SalesReportTm, Double> colSalesNetRevenue;
    
    @FXML
    private TableColumn<SalesReportTm, Double> colSalesAvgOrder;
    
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
    private TableColumn<TopProductTm, Integer> colProductReturns;
    
    @FXML
    private TableColumn<TopProductTm, Integer> colProductNetQty;
    
    @FXML
    private TableColumn<TopProductTm, Double> colProductRevenue;
    
    @FXML
    private TableColumn<TopProductTm, Double> colProductRefunds;
    
    @FXML
    private TableColumn<TopProductTm, Double> colProductNetRevenue;
    
    @FXML
    private TableColumn<TopProductTm, Double> colProductAvgPrice;
    
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
    private TableColumn<CategoryReportTm, Double> colCategoryRefunds;
    
    @FXML
    private TableColumn<CategoryReportTm, Double> colCategoryNetRevenue;
    
    @FXML
    private TableColumn<CategoryReportTm, Double> colCategoryAvgOrder;
    
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
    
    @FXML
    private TableColumn<CashierReportTm, Double> colCashierRefunds;
    
    @FXML
    private TableColumn<CashierReportTm, Double> colCashierNetRevenue;
    
    @FXML
    private TableColumn<CashierReportTm, Double> colCashierAvgOrder;
    
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
    
    // Product Performance Tab
    @FXML
    private javafx.scene.control.ComboBox<Product> cmbProductPerformance;
    
    @FXML
    private Text lblProductTotalSold;
    
    @FXML
    private Text lblProductTotalRevenue;
    
    @FXML
    private Text lblProductUniqueCustomers;
    
    @FXML
    private Text lblProductAvgQty;
    
    @FXML
    private TableView<ProductSalesHistoryTm> tblProductSalesHistory;
    
    @FXML
    private TableColumn<ProductSalesHistoryTm, Long> colProductOrderId;
    
    @FXML
    private TableColumn<ProductSalesHistoryTm, String> colProductSaleDate;
    
    @FXML
    private TableColumn<ProductSalesHistoryTm, Integer> colProductQty;
    
//    @FXML
//    private TableColumn<ProductSalesHistoryTm, Double> colProductRevenue;
    
    @FXML
    private TableView<ProductSalesTrendTm> tblProductSalesTrend;
    
    @FXML
    private TableColumn<ProductSalesTrendTm, String> colTrendDate;
    
    @FXML
    private TableColumn<ProductSalesTrendTm, Integer> colTrendQty;
    
    @FXML
    private TableColumn<ProductSalesTrendTm, Double> colTrendRevenue;
    
    private final OrderDetailService orderDetailService;
    private final OrderItemService orderItemService;
    private final ProductRepository productRepository;
    private final ProductDetailRepository productDetailRepository;
    private final PDFReportService pdfReportService;
    private final ReturnOrderService returnOrderService;
    private final ReturnOrderItemService returnOrderItemService;
    
    private LocalDateTime filterStartDate = null;
    private LocalDateTime filterEndDate = null;
    
    @FXML
    public void initialize() {
        // Initialize sidebar
        initializeSidebar();
        
        // Authorization check: Reports accessible by ADMIN only
        if (!com.devstack.pos.util.AuthorizationUtil.canAccessReports()) {
            com.devstack.pos.util.AuthorizationUtil.showAdminOnlyAlert();
            // Navigate back to dashboard
            javafx.application.Platform.runLater(() -> {
                btnDashboardOnAction(null);
            });
            return;
        }
        
        // Initialize date pickers
        dateFrom.setValue(LocalDate.now().minusMonths(1));
        dateTo.setValue(LocalDate.now());
        
        // Configure Sales Reports Table
        colSalesPeriod.setCellValueFactory(new PropertyValueFactory<>("period"));
        colSalesOrders.setCellValueFactory(new PropertyValueFactory<>("orders"));
        colSalesRevenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        colSalesRefunds.setCellValueFactory(new PropertyValueFactory<>("refunds"));
        colSalesNetRevenue.setCellValueFactory(new PropertyValueFactory<>("netRevenue"));
        colSalesAvgOrder.setCellValueFactory(new PropertyValueFactory<>("avgOrder"));
        colSalesProfit.setCellValueFactory(new PropertyValueFactory<>("profit"));
        
        // Format currency columns for Sales Reports
        formatCurrencyColumn(colSalesRevenue);
        formatCurrencyColumn(colSalesRefunds);
        formatCurrencyColumn(colSalesNetRevenue);
        formatCurrencyColumn(colSalesAvgOrder);
        formatCurrencyColumn(colSalesProfit);
        
        // Configure Top Products Table
        colProductRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colProductQtySold.setCellValueFactory(new PropertyValueFactory<>("qtySold"));
        colProductReturns.setCellValueFactory(new PropertyValueFactory<>("returns"));
        colProductNetQty.setCellValueFactory(new PropertyValueFactory<>("netQty"));
        colProductRevenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        colProductRefunds.setCellValueFactory(new PropertyValueFactory<>("refunds"));
        colProductNetRevenue.setCellValueFactory(new PropertyValueFactory<>("netRevenue"));
        colProductAvgPrice.setCellValueFactory(new PropertyValueFactory<>("avgPrice"));
        
        // Format currency columns for Top Products
        formatCurrencyColumn(colProductRevenue);
        formatCurrencyColumn(colProductRefunds);
        formatCurrencyColumn(colProductNetRevenue);
        formatCurrencyColumn(colProductAvgPrice);
        
        // Configure Sales by Category Table
        colCategoryName.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colCategoryOrders.setCellValueFactory(new PropertyValueFactory<>("orders"));
        colCategoryRevenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        colCategoryRefunds.setCellValueFactory(new PropertyValueFactory<>("refunds"));
        colCategoryNetRevenue.setCellValueFactory(new PropertyValueFactory<>("netRevenue"));
        colCategoryAvgOrder.setCellValueFactory(new PropertyValueFactory<>("avgOrder"));
        colCategoryProfit.setCellValueFactory(new PropertyValueFactory<>("profit"));
        
        // Format currency columns for Sales by Category
        formatCurrencyColumn(colCategoryRevenue);
        formatCurrencyColumn(colCategoryRefunds);
        formatCurrencyColumn(colCategoryNetRevenue);
        formatCurrencyColumn(colCategoryAvgOrder);
        formatCurrencyColumn(colCategoryProfit);
        
        // Configure Sales by Cashier Table
        colCashierRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colCashierName.setCellValueFactory(new PropertyValueFactory<>("cashierName"));
        colCashierOrders.setCellValueFactory(new PropertyValueFactory<>("orders"));
        colCashierRevenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        colCashierRefunds.setCellValueFactory(new PropertyValueFactory<>("refunds"));
        colCashierNetRevenue.setCellValueFactory(new PropertyValueFactory<>("netRevenue"));
        colCashierAvgOrder.setCellValueFactory(new PropertyValueFactory<>("avgOrder"));
        
        // Format currency columns for Sales by Cashier
        formatCurrencyColumn(colCashierRevenue);
        formatCurrencyColumn(colCashierRefunds);
        formatCurrencyColumn(colCashierNetRevenue);
        formatCurrencyColumn(colCashierAvgOrder);
        
        // Configure Profit & Loss Table
        colPLProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colPLQtySold.setCellValueFactory(new PropertyValueFactory<>("qtySold"));
        colPLCost.setCellValueFactory(new PropertyValueFactory<>("cost"));
        colPLRevenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        colPLProfit.setCellValueFactory(new PropertyValueFactory<>("profit"));
        
        // Set CONSTRAINED_RESIZE_POLICY for all tables
        tblSalesReports.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblTopProducts.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblSalesByCategory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblSalesByCashier.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblProfitLoss.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // Load all reports
        loadAllReports();
        
        // Load weekly sales by default
        loadWeeklySales(null);
        
        // Initialize Product Performance Tab
        initializeProductPerformanceTab();
    }
    
    private void initializeProductPerformanceTab() {
        // Load products into combo box
        if (cmbProductPerformance != null) {
            ObservableList<Product> products = FXCollections.observableArrayList(
                productRepository.findAll()
            );
            cmbProductPerformance.setItems(products);
            
            cmbProductPerformance.setCellFactory(param -> new javafx.scene.control.ListCell<Product>() {
                @Override
                protected void updateItem(Product item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getCode() + " - " + item.getDescription());
                    }
                }
            });
            
            cmbProductPerformance.setButtonCell(new javafx.scene.control.ListCell<Product>() {
                @Override
                protected void updateItem(Product item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getCode() + " - " + item.getDescription());
                    }
                }
            });
        }
        
        // Configure product sales history table
        if (colProductOrderId != null) {
            colProductOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
            colProductSaleDate.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
            colProductQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            colProductRevenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));
            formatCurrencyColumn(colProductRevenue);
            tblProductSalesHistory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
        
        // Configure product sales trend table
        if (colTrendDate != null) {
            colTrendDate.setCellValueFactory(new PropertyValueFactory<>("date"));
            colTrendQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            colTrendRevenue.setCellValueFactory(new PropertyValueFactory<>("revenue"));
            formatCurrencyColumn(colTrendRevenue);
            tblProductSalesTrend.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
    }
    
    @FXML
    public void refreshAll(ActionEvent event) {
        loadAllReports();
        // Reload weekly sales to maintain default view
        loadWeeklySales(null);
    }
    
    // PDF Download Methods
    @FXML
    public void downloadSalesReportPDF(ActionEvent event) {
        try {
            String filePath = pdfReportService.generateSalesReportPDF(
                filterStartDate != null ? filterStartDate : null,
                filterEndDate != null ? filterEndDate : null,
                "comprehensive"
            );
            showSuccessAlert("PDF Generated", "Sales report PDF has been saved to:\n" + filePath);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("PDF Generation Error", "Failed to generate sales report PDF: " + e.getMessage());
        }
    }
    
    @FXML
    public void downloadReturnOrdersReportPDF(ActionEvent event) {
        try {
            String filePath = pdfReportService.generateReturnOrdersReportPDF(
                filterStartDate != null ? filterStartDate : null,
                filterEndDate != null ? filterEndDate : null
            );
            showSuccessAlert("PDF Generated", "Return orders report PDF has been saved to:\n" + filePath);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("PDF Generation Error", "Failed to generate return orders report PDF: " + e.getMessage());
        }
    }
    
    @FXML
    public void downloadInventoryReportPDF(ActionEvent event) {
        try {
            String filePath = pdfReportService.generateInventoryReportPDF();
            showSuccessAlert("PDF Generated", "Inventory report PDF has been saved to:\n" + filePath);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("PDF Generation Error", "Failed to generate inventory report PDF: " + e.getMessage());
        }
    }
    
    @FXML
    public void downloadFinancialReportPDF(ActionEvent event) {
        try {
            String filePath = pdfReportService.generateFinancialReportPDF(
                filterStartDate != null ? filterStartDate : null,
                filterEndDate != null ? filterEndDate : null
            );
            showSuccessAlert("PDF Generated", "Financial report PDF has been saved to:\n" + filePath);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("PDF Generation Error", "Failed to generate financial report PDF: " + e.getMessage());
        }
    }
    
    @FXML
    public void downloadSupplierReportPDF(ActionEvent event) {
        try {
            String filePath = pdfReportService.generateSupplierReportPDF();
            showSuccessAlert("PDF Generated", "Supplier report PDF has been saved to:\n" + filePath);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("PDF Generation Error", "Failed to generate supplier report PDF: " + e.getMessage());
        }
    }
    
    @FXML
    public void downloadComprehensiveReportPDF(ActionEvent event) {
        try {
            String filePath = pdfReportService.generateComprehensiveReportPDF(
                filterStartDate != null ? filterStartDate : null,
                filterEndDate != null ? filterEndDate : null
            );
            showSuccessAlert("PDF Generated", "Comprehensive report PDF has been saved to:\n" + filePath);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("PDF Generation Error", "Failed to generate comprehensive report PDF: " + e.getMessage());
        }
    }
    
    private void showSuccessAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showErrorAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Helper method to format currency columns
     */
    private <T> void formatCurrencyColumn(TableColumn<T, Double> column) {
        column.setCellFactory(col -> new javafx.scene.control.TableCell<T, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f /=", value));
                }
            }
        });
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
    }
    
    private void loadSummaryStatistics() {
        Double revenue;
        Double refundAmount;
        Double netRevenue;
        Long orders;
        Double profit;
        Double avgOrder;
        
        if (filterStartDate != null && filterEndDate != null) {
            revenue = orderDetailService.getRevenueByDateRange(filterStartDate, filterEndDate);
            refundAmount = returnOrderService.getTotalRefundAmountByDateRange(filterStartDate, filterEndDate);
            netRevenue = (revenue != null ? revenue : 0.0) - (refundAmount != null ? refundAmount : 0.0);
            orders = orderDetailService.countOrdersByDateRange(filterStartDate, filterEndDate);
            avgOrder = orderDetailService.getAverageOrderValueByDateRange(filterStartDate, filterEndDate);
        } else {
            revenue = orderDetailService.getTotalRevenue();
            refundAmount = returnOrderService.getTotalRefundAmount();
            netRevenue = (revenue != null ? revenue : 0.0) - (refundAmount != null ? refundAmount : 0.0);
            orders = orderDetailService.getTotalOrderCount();
            avgOrder = orderDetailService.getAverageOrderValue();
        }
        
        // Calculate profit (net revenue - cost, simplified for now)
        profit = netRevenue; // This can be enhanced with actual cost calculations
        
        lblTotalRevenue.setText(String.format("%.2f /=", netRevenue != null ? netRevenue : 0.0));
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
            Double refundAmount = returnOrderService.getTotalRefundAmountByDateRange(startDateTime, endDateTime);
            Double netRevenue = (revenue != null ? revenue : 0.0) - (refundAmount != null ? refundAmount : 0.0);
            Long orders = orderDetailService.countOrdersByDateRange(startDateTime, endDateTime);
            Double avgOrder = orders != null && orders > 0 ? (netRevenue / orders) : 0.0;

            String period = weekStart.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " + 
                          weekEnd.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            double profit = netRevenue != null ? netRevenue : 0.0; // Net revenue as profit (can be enhanced with cost calculations)
            data.add(new SalesReportTm(period, orders != null ? orders.intValue() : 0, 
                revenue != null ? revenue : 0.0, refundAmount != null ? refundAmount : 0.0,
                netRevenue != null ? netRevenue : 0.0, avgOrder, profit));
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
            Double refundAmount = returnOrderService.getTotalRefundAmountByDateRange(startDateTime, endDateTime);
            Double netRevenue = (revenue != null ? revenue : 0.0) - (refundAmount != null ? refundAmount : 0.0);
            Long orders = orderDetailService.countOrdersByDateRange(startDateTime, endDateTime);
            Double avgOrder = orders != null && orders > 0 ? (netRevenue / orders) : 0.0;

            String period = monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            double profit = netRevenue != null ? netRevenue : 0.0; // Net revenue as profit (can be enhanced with cost calculations)
            data.add(new SalesReportTm(period, orders != null ? orders.intValue() : 0, 
                revenue != null ? revenue : 0.0, refundAmount != null ? refundAmount : 0.0,
                netRevenue != null ? netRevenue : 0.0, avgOrder, profit));
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
            Double refundAmount = returnOrderService.getTotalRefundAmountByDateRange(startDateTime, endDateTime);
            Double netRevenue = (revenue != null ? revenue : 0.0) - (refundAmount != null ? refundAmount : 0.0);
            Long orders = orderDetailService.countOrdersByDateRange(startDateTime, endDateTime);
            Double avgOrder = orders != null && orders > 0 ? (netRevenue / orders) : 0.0;

            String period = String.valueOf(yearStart.getYear());
            double profit = netRevenue != null ? netRevenue : 0.0; // Net revenue as profit (can be enhanced with cost calculations)
            data.add(new SalesReportTm(period, orders != null ? orders.intValue() : 0, 
                revenue != null ? revenue : 0.0, refundAmount != null ? refundAmount : 0.0,
                netRevenue != null ? netRevenue : 0.0, avgOrder, profit));
        }
        
        tblSalesReports.setItems(data);
    }
    
    private void loadTopProducts() {
        List<Object[]> topProductsData;
        
        if (filterStartDate != null && filterEndDate != null) {
            topProductsData = orderItemService.getTopSellingProductsWithRevenue(filterStartDate, filterEndDate);
        } else {
            topProductsData = orderItemService.getTopSellingProductsWithRevenue();
        }
        
        // Get refunds and returned quantities by product
        List<Object[]> refundsAndQuantitiesByProduct;
        if (filterStartDate != null && filterEndDate != null) {
            refundsAndQuantitiesByProduct = returnOrderItemService.getRefundsAndQuantitiesByProductByDateRange(filterStartDate, filterEndDate);
        } else {
            refundsAndQuantitiesByProduct = returnOrderItemService.getRefundsAndQuantitiesByProduct();
        }
        
        // Create maps for quick lookup
        Map<Integer, Double> refundMap = new HashMap<>();
        Map<Integer, Integer> returnedQtyMap = new HashMap<>();
        
        for (Object[] refundData : refundsAndQuantitiesByProduct) {
            Integer productCode = ((Number) refundData[0]).intValue();
            Double refundAmount = ((Number) refundData[1]).doubleValue();
            Integer returnedQty = ((Number) refundData[2]).intValue();
            
            refundMap.put(productCode, refundAmount);
            returnedQtyMap.put(productCode, returnedQty);
        }
        
        ObservableList<TopProductTm> observableList = FXCollections.observableArrayList();
        
        int rank = 1;
        for (Object[] data : topProductsData) {
            Integer productCode = ((Number) data[0]).intValue();
            String productName = (String) data[1];
            Integer qtySold = ((Number) data[2]).intValue();
            Double revenue = ((Number) data[3]).doubleValue();
            
            // Get refund amount and returned quantity for this product
            Double refundAmount = refundMap.getOrDefault(productCode, 0.0);
            Integer returnedQty = returnedQtyMap.getOrDefault(productCode, 0);
            
            // Calculate net values
            Integer netQtySold = (qtySold != null ? qtySold : 0) - (returnedQty != null ? returnedQty : 0);
            Double netRevenue = (revenue != null ? revenue : 0.0) - (refundAmount != null ? refundAmount : 0.0);
            Double avgPrice = (netQtySold != null && netQtySold > 0) ? (netRevenue / netQtySold) : 0.0;
            
            // Only show products with net quantity > 0 or net revenue > 0
            if (netQtySold > 0 || netRevenue > 0) {
                TopProductTm tm = new TopProductTm(rank++, productName != null ? productName : "Unknown", 
                    qtySold, returnedQty, netQtySold, revenue, refundAmount, netRevenue, avgPrice);
                observableList.add(tm);
            }
        }
        
        tblTopProducts.setItems(observableList);
    }
    
    private void loadSalesByCategory() {
        List<Object[]> categoryData;
        
        if (filterStartDate != null && filterEndDate != null) {
            categoryData = orderItemService.getSalesByCategory(filterStartDate, filterEndDate);
        } else {
            categoryData = orderItemService.getSalesByCategory();
        }
        
        // Get refunds by category
        List<Object[]> refundsByCategory;
        if (filterStartDate != null && filterEndDate != null) {
            refundsByCategory = returnOrderItemService.getRefundsByCategoryByDateRange(filterStartDate, filterEndDate);
        } else {
            refundsByCategory = returnOrderItemService.getRefundsByCategory();
        }
        
        // Create a map of category name to refund amount
        Map<String, Double> refundMap = new HashMap<>();
        for (Object[] refundData : refundsByCategory) {
            String categoryName = (String) refundData[0];
            Double refundAmount = ((Number) refundData[1]).doubleValue();
            refundMap.put(categoryName != null ? categoryName : "Uncategorized", refundAmount);
        }
        
        ObservableList<CategoryReportTm> observableList = FXCollections.observableArrayList();
        
        for (Object[] data : categoryData) {
            String categoryName = (String) data[0];
            String categoryKey = categoryName != null ? categoryName : "Uncategorized";
            Long orderCount = ((Number) data[1]).longValue();
            Double revenue = ((Number) data[2]).doubleValue();
            
            // Get refund amount for this category
            Double refundAmount = refundMap.getOrDefault(categoryKey, 0.0);
            Double netRevenue = (revenue != null ? revenue : 0.0) - (refundAmount != null ? refundAmount : 0.0);
            Double avgOrder = orderCount.intValue() > 0 ? (netRevenue / orderCount.intValue()) : 0.0;
            Double profit = netRevenue; // Net revenue as profit
            
            CategoryReportTm tm = new CategoryReportTm(
                categoryKey,
                orderCount.intValue(),
                revenue,
                refundAmount,
                netRevenue,
                avgOrder,
                profit
            );
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
        
        // Get total refunds for the period (cashier-level refunds would require order-level tracking)
        // For now, we'll use a proportional approach or show gross revenue
        // Note: Cashier refund tracking would need order-to-cashier mapping in return orders
        Double totalRefunds;
        if (filterStartDate != null && filterEndDate != null) {
            totalRefunds = returnOrderService.getTotalRefundAmountByDateRange(filterStartDate, filterEndDate);
        } else {
            totalRefunds = returnOrderService.getTotalRefundAmount();
        }
        
        // Calculate total revenue to distribute refunds proportionally
        Double totalRevenue = cashierData.stream()
            .mapToDouble(data -> ((Number) data[2]).doubleValue())
            .sum();
        
        ObservableList<CashierReportTm> observableList = FXCollections.observableArrayList();
        
        int rank = 1;
        for (Object[] data : cashierData) {
            String cashierEmail = (String) data[0];
            Integer orders = ((Number) data[1]).intValue();
            Double revenue = ((Number) data[2]).doubleValue();
            
            // Calculate proportional refund (simplified - ideally would track by cashier)
            Double proportionalRefund = 0.0;
            if (totalRevenue != null && totalRevenue > 0 && totalRefunds != null) {
                proportionalRefund = (revenue / totalRevenue) * totalRefunds;
            }
            Double netRevenue = (revenue != null ? revenue : 0.0) - proportionalRefund;
            Double avgOrder = orders != null && orders > 0 ? (netRevenue / orders) : 0.0;
            
            CashierReportTm tm = new CashierReportTm(rank++, cashierEmail != null ? cashierEmail : "Unknown", 
                orders, revenue, proportionalRefund, netRevenue, avgOrder);
            observableList.add(tm);
        }
        
        tblSalesByCashier.setItems(observableList);
    }
    
    private void loadProfitLoss() {
        Double totalSales;
        Double refundAmount;
        Double netSales;
        Double costOfGoods;
        Double grossProfit;
        
        if (filterStartDate != null && filterEndDate != null) {
            totalSales = orderDetailService.getRevenueByDateRange(filterStartDate, filterEndDate);
            refundAmount = returnOrderService.getTotalRefundAmountByDateRange(filterStartDate, filterEndDate);
            netSales = (totalSales != null ? totalSales : 0.0) - (refundAmount != null ? refundAmount : 0.0);
            costOfGoods = 0.0;
            grossProfit = netSales; // Net sales as profit (can be enhanced with cost calculations)
        } else {
            totalSales = orderDetailService.getTotalRevenue();
            refundAmount = returnOrderService.getTotalRefundAmount();
            netSales = (totalSales != null ? totalSales : 0.0) - (refundAmount != null ? refundAmount : 0.0);
            costOfGoods = 0.0;
            grossProfit = netSales; // Net sales as profit (can be enhanced with cost calculations)
        }
        
        lblPLTotalSales.setText(String.format("%.2f /=", netSales != null ? netSales : 0.0));
        lblPLCostOfGoods.setText(String.format("%.2f /=", costOfGoods != null ? costOfGoods : 0.0));
        lblPLGrossProfit.setText(String.format("%.2f /=", grossProfit != null ? grossProfit : 0.0));
        
        double profitMargin = (netSales != null && netSales > 0) ? (grossProfit / netSales * 100) : 0.0;
        lblPLProfitMargin.setText(String.format("%.2f%%", profitMargin));
        
        // Load detailed profit by product
        tblProfitLoss.setItems(FXCollections.observableArrayList());
    }
    
    // ===== Table Model Classes =====
    
    public static class SalesReportTm {
        private String period;
        private int orders;
        private double revenue;
        private double refunds;
        private double netRevenue;
        private double avgOrder;
        private double profit;
        
        public SalesReportTm(String period, int orders, double revenue, double refunds, double netRevenue, double avgOrder, double profit) {
            this.period = period;
            this.orders = orders;
            this.revenue = revenue;
            this.refunds = refunds;
            this.netRevenue = netRevenue;
            this.avgOrder = avgOrder;
            this.profit = profit;
        }
        
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public int getOrders() { return orders; }
        public void setOrders(int orders) { this.orders = orders; }
        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
        public double getRefunds() { return refunds; }
        public void setRefunds(double refunds) { this.refunds = refunds; }
        public double getNetRevenue() { return netRevenue; }
        public void setNetRevenue(double netRevenue) { this.netRevenue = netRevenue; }
        public double getAvgOrder() { return avgOrder; }
        public void setAvgOrder(double avgOrder) { this.avgOrder = avgOrder; }
        public double getProfit() { return profit; }
        public void setProfit(double profit) { this.profit = profit; }
    }
    
    public static class TopProductTm {
        private int rank;
        private String productName;
        private int qtySold;
        private int returns;
        private int netQty;
        private double revenue;
        private double refunds;
        private double netRevenue;
        private double avgPrice;
        
        public TopProductTm(int rank, String productName, int qtySold, int returns, int netQty, double revenue, double refunds, double netRevenue, double avgPrice) {
            this.rank = rank;
            this.productName = productName;
            this.qtySold = qtySold;
            this.returns = returns;
            this.netQty = netQty;
            this.revenue = revenue;
            this.refunds = refunds;
            this.netRevenue = netRevenue;
            this.avgPrice = avgPrice;
        }
        
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getQtySold() { return qtySold; }
        public void setQtySold(int qtySold) { this.qtySold = qtySold; }
        public int getReturns() { return returns; }
        public void setReturns(int returns) { this.returns = returns; }
        public int getNetQty() { return netQty; }
        public void setNetQty(int netQty) { this.netQty = netQty; }
        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
        public double getRefunds() { return refunds; }
        public void setRefunds(double refunds) { this.refunds = refunds; }
        public double getNetRevenue() { return netRevenue; }
        public void setNetRevenue(double netRevenue) { this.netRevenue = netRevenue; }
        public double getAvgPrice() { return avgPrice; }
        public void setAvgPrice(double avgPrice) { this.avgPrice = avgPrice; }
    }
    
    public static class CategoryReportTm {
        private String categoryName;
        private int orders;
        private double revenue;
        private double refunds;
        private double netRevenue;
        private double avgOrder;
        private double profit;
        
        public CategoryReportTm(String categoryName, int orders, double revenue, double refunds, double netRevenue, double avgOrder, double profit) {
            this.categoryName = categoryName;
            this.orders = orders;
            this.revenue = revenue;
            this.refunds = refunds;
            this.netRevenue = netRevenue;
            this.avgOrder = avgOrder;
            this.profit = profit;
        }
        
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public int getOrders() { return orders; }
        public void setOrders(int orders) { this.orders = orders; }
        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
        public double getRefunds() { return refunds; }
        public void setRefunds(double refunds) { this.refunds = refunds; }
        public double getNetRevenue() { return netRevenue; }
        public void setNetRevenue(double netRevenue) { this.netRevenue = netRevenue; }
        public double getAvgOrder() { return avgOrder; }
        public void setAvgOrder(double avgOrder) { this.avgOrder = avgOrder; }
        public double getProfit() { return profit; }
        public void setProfit(double profit) { this.profit = profit; }
    }
    
    public static class CashierReportTm {
        private int rank;
        private String cashierName;
        private int orders;
        private double revenue;
        private double refunds;
        private double netRevenue;
        private double avgOrder;
        
        public CashierReportTm(int rank, String cashierName, int orders, double revenue, double refunds, double netRevenue, double avgOrder) {
            this.rank = rank;
            this.cashierName = cashierName;
            this.orders = orders;
            this.revenue = revenue;
            this.refunds = refunds;
            this.netRevenue = netRevenue;
            this.avgOrder = avgOrder;
        }
        
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public String getCashierName() { return cashierName; }
        public void setCashierName(String cashierName) { this.cashierName = cashierName; }
        public int getOrders() { return orders; }
        public void setOrders(int orders) { this.orders = orders; }
        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
        public double getRefunds() { return refunds; }
        public void setRefunds(double refunds) { this.refunds = refunds; }
        public double getNetRevenue() { return netRevenue; }
        public void setNetRevenue(double netRevenue) { this.netRevenue = netRevenue; }
        public double getAvgOrder() { return avgOrder; }
        public void setAvgOrder(double avgOrder) { this.avgOrder = avgOrder; }
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
    
    // Product Performance Methods
    @FXML
    public void loadProductPerformance(ActionEvent event) {
        Product selectedProduct = cmbProductPerformance.getValue();
        if (selectedProduct == null) {
            showErrorAlert("No Product Selected", "Please select a product to view performance analytics.");
            return;
        }
        
        Integer productCode = selectedProduct.getCode();
        
        // Load summary statistics (supports decimal quantities)
        Double totalSold = orderItemService.getTotalQuantitySoldByProduct(productCode);
        Double totalRevenue = orderItemService.getTotalRevenueByProduct(productCode);
        Long uniqueCustomers = orderItemService.getProductUniqueCustomers(productCode);
        Double avgQtyPerOrder = orderItemService.getProductAverageQuantityPerOrder(productCode);
        
        if (lblProductTotalSold != null) {
            // Format quantity - show as integer if whole number, otherwise show decimals
            if (totalSold != null) {
                if (totalSold == totalSold.intValue()) {
                    lblProductTotalSold.setText(String.valueOf(totalSold.intValue()));
                } else {
                    lblProductTotalSold.setText(String.format("%.2f", totalSold));
                }
            } else {
                lblProductTotalSold.setText("0");
            }
        }
        if (lblProductTotalRevenue != null) {
            lblProductTotalRevenue.setText(String.format("%.2f /=", totalRevenue != null ? totalRevenue : 0.0));
        }
        if (lblProductUniqueCustomers != null) {
            lblProductUniqueCustomers.setText(String.valueOf(uniqueCustomers != null ? uniqueCustomers : 0));
        }
        if (lblProductAvgQty != null) {
            lblProductAvgQty.setText(String.format("%.2f", avgQtyPerOrder != null ? avgQtyPerOrder : 0.0));
        }
        
        // Load sales history
        List<Object[]> salesHistory = orderItemService.getProductSalesHistory(productCode);
        ObservableList<ProductSalesHistoryTm> historyList = FXCollections.observableArrayList();
        for (Object[] data : salesHistory) {
            Long orderId = ((Number) data[0]).longValue();
            LocalDateTime saleDate = (LocalDateTime) data[1];
            Integer qty = ((Number) data[2]).intValue();
            Double revenue = ((Number) data[3]).doubleValue();
            historyList.add(new ProductSalesHistoryTm(
                orderId,
                saleDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                qty,
                revenue
            ));
        }
        tblProductSalesHistory.setItems(historyList);
        
        // Load sales trend
        List<Object[]> salesTrend = orderItemService.getProductSalesTrendByDate(productCode);
        ObservableList<ProductSalesTrendTm> trendList = FXCollections.observableArrayList();
        for (Object[] data : salesTrend) {
            Object dateObj = data[0];
            String dateStr;
            if (dateObj instanceof java.sql.Date) {
                dateStr = ((java.sql.Date) dateObj).toString();
            } else if (dateObj instanceof java.util.Date) {
                dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format((java.util.Date) dateObj);
            } else {
                dateStr = dateObj != null ? dateObj.toString() : "N/A";
            }
            Integer qty = data[1] != null ? ((Number) data[1]).intValue() : 0;
            Double revenue = data[2] != null ? ((Number) data[2]).doubleValue() : 0.0;
            trendList.add(new ProductSalesTrendTm(dateStr, qty, revenue));
        }
        tblProductSalesTrend.setItems(trendList);
    }
    
    // Table Model Classes for Product Performance
    public static class ProductSalesHistoryTm {
        private Long orderId;
        private String saleDate;
        private int quantity;
        private double revenue;
        
        public ProductSalesHistoryTm(Long orderId, String saleDate, int quantity, double revenue) {
            this.orderId = orderId;
            this.saleDate = saleDate;
            this.quantity = quantity;
            this.revenue = revenue;
        }
        
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public String getSaleDate() { return saleDate; }
        public void setSaleDate(String saleDate) { this.saleDate = saleDate; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
    }
    
    public static class ProductSalesTrendTm {
        private String date;
        private int quantity;
        private double revenue;
        
        public ProductSalesTrendTm(String date, int quantity, double revenue) {
            this.date = date;
            this.quantity = quantity;
            this.revenue = revenue;
        }
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getRevenue() { return revenue; }
        public void setRevenue(double revenue) { this.revenue = revenue; }
    }
}
