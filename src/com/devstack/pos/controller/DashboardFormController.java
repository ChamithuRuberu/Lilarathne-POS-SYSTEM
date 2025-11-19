package com.devstack.pos.controller;

import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.entity.ReturnOrder;
import com.devstack.pos.entity.ReturnOrderItem;
import com.devstack.pos.util.AuthorizationUtil;
import com.devstack.pos.util.UserSessionData;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.OrderItemService;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.CustomerService;
import com.devstack.pos.service.ReturnOrderService;
import com.devstack.pos.service.ReturnOrderItemService;
import com.devstack.pos.service.ProductService;
import com.devstack.pos.service.CategoryService;
import com.jfoenix.controls.JFXButton;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DashboardFormController extends BaseController {
    
    private final OrderDetailService orderDetailService;
    private final OrderItemService orderItemService;
    private final ProductDetailService productDetailService;
    private final CustomerService customerService;
    private final ReturnOrderService returnOrderService;
    private final ReturnOrderItemService returnOrderItemService;
    private final ProductService productService;
    private final CategoryService categoryService;
    
    // KPI Labels
    @FXML
    private Text lblTodayRevenue;
    
    @FXML
    private Text lblTotalCustomers;
    
    @FXML
    private Text lblTotalProducts;
    
    @FXML
    private Text lblRevenueChange;
    
    @FXML
    private Text lblWelcomeMessage;
    
    @FXML
    private BarChart<String, Number> barChartTopProducts;
    
    @FXML
    private CategoryAxis topProductsCategoryAxis;
    
    @FXML
    private NumberAxis topProductsNumberAxis;
    
    @FXML
    private BarChart<String, Number> barChartMonthlyIncome;
    
    @FXML
    private CategoryAxis categoryAxis;
    
    @FXML
    private NumberAxis numberAxis;
    
    @FXML
    private TableView<RecentTransactionTm> tblRecentTransactions;
    
    @FXML
    private Text lblTopProductName;
    
    @FXML
    private Text lblTopProductStats;
    
    @FXML
    private Text lblPendingReturns;
    
    @FXML
    private Text lblPendingRefunds;
    
    @FXML
    private Text lblTodayReturns;
    
    @FXML
    private Text lblTodayOrders;
    
    @FXML
    private Text lblOrdersChange;
    
    @FXML
    private Text lblAvgOrderValue;
    
    @FXML
    private Text lblTotalProductsCount;
    
    @FXML
    private Text lblActiveProducts;
    
    @FXML
    private Text lblLowStockCount;
    
    @FXML
    private Text lblPendingPaymentsCount;
    
    @FXML
    private Text lblPendingPaymentsAmount;
    
    @FXML
    private Text lblPendingPaymentsDetails;
    
    @FXML
    private MenuButton btnUserMenu;
    
    @FXML
    private MenuItem menuItemSettings;
    
    @FXML
    private MenuItem menuItemLogout;
    
    // Sidebar buttons for role-based visibility
    @FXML
    private JFXButton btnProduct;
    
    @FXML
    private JFXButton btnCustomer;
    
    @FXML
    private JFXButton btnPlaceOrder;
    
    @FXML
    private JFXButton btnReturns;
    
    @FXML
    private JFXButton btnOrderDetails;
    
    @FXML
    private JFXButton btnPurchase;
    
    @FXML
    private JFXButton btnReports;
    
    @FXML
    public void initialize() {
        // Initialize sidebar with user info
        initializeSidebar();
        
        // Setup user menu actions
        setupUserMenuActions();
        
        // Configure menu visibility based on user role
        configureMenuVisibility();
        
        System.out.println("Dashboard loaded for user: " + UserSessionData.email + " with role: " + UserSessionData.userRole);
        
        // Load all dashboard data on initialization
        // This is called every time the dashboard is navigated to (new controller instance)
        refreshDashboard();
        
        // Ensure dashboard refreshes when scene becomes visible
        // Use Platform.runLater to ensure scene is fully initialized
        javafx.application.Platform.runLater(() -> {
            try {
                if (context != null) {
                    // Add listener for when scene is set
                    context.sceneProperty().addListener((obs, oldScene, newScene) -> {
                        if (newScene != null) {
                            // Refresh when scene becomes visible
                            javafx.application.Platform.runLater(() -> {
                                System.out.println("Dashboard scene loaded - refreshing data");
                                refreshDashboard();
                            });
                        }
                    });
                    
                    // If scene is already set, refresh now
                    if (context.getScene() != null) {
                        javafx.application.Platform.runLater(() -> {
                            System.out.println("Dashboard scene already set - refreshing data");
                            refreshDashboard();
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("Error setting up dashboard refresh listener: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Configure menu visibility based on user role
     * Normal users: Customers, POS/Orders, Return Orders, All Orders
     * Admin users: All features including Products, Supplier, Settings
     */
    private void configureMenuVisibility() {
        boolean isAdmin = AuthorizationUtil.isAdmin();
        
        // Always visible for all users (normal and admin)
        if (btnCustomer != null) {
            btnCustomer.setVisible(true);
            btnCustomer.setManaged(true);
        }
        if (btnPlaceOrder != null) {
            btnPlaceOrder.setVisible(true);
            btnPlaceOrder.setManaged(true);
        }
        if (btnReturns != null) {
            btnReturns.setVisible(true);
            btnReturns.setManaged(true);
        }
        if (btnOrderDetails != null) {
            btnOrderDetails.setVisible(true);
            btnOrderDetails.setManaged(true);
        }
        
        // Admin-only features
        if (btnProduct != null) {
            btnProduct.setVisible(isAdmin);
            btnProduct.setManaged(isAdmin);
        }
        if (btnPurchase != null) {
            btnPurchase.setVisible(isAdmin);
            btnPurchase.setManaged(isAdmin);
        }
        if (btnReports != null) {
            btnReports.setVisible(isAdmin);
            btnReports.setManaged(isAdmin);
        }
        
        // Settings menu item - Admin only
        if (menuItemSettings != null) {
            menuItemSettings.setVisible(isAdmin);
        }
    }
    
    /**
     * Refreshes all dashboard data
     * Called on initialization, when refresh button is clicked, and when navigating to dashboard
     */
    public void refreshDashboard() {
        // Ensure this runs on JavaFX Application Thread
        if (javafx.application.Platform.isFxApplicationThread()) {
            refreshDashboardInternal();
        } else {
            javafx.application.Platform.runLater(this::refreshDashboardInternal);
        }
    }
    
    private void refreshDashboardInternal() {
        try {
            System.out.println("Refreshing dashboard data at " + java.time.LocalDateTime.now());
            loadWelcomeMessage();
            loadKpis();
            loadTopSellingProductsChart();
            loadMonthlyIncomeChart();
            loadRecentTransactions();
            loadQuickStats();
            loadPendingTasks(); // Load return orders statistics
            loadPendingPayments(); // Load pending payments statistics
            System.out.println("Dashboard refresh completed");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error refreshing dashboard: " + ex.getMessage());
        }
    }
    
    @Override
    protected String getCurrentPageName() {
        return "Dashboard";
    }
    
    
    // Dashboard-specific action handlers
    
    // Alias for FXML compatibility (DashboardForm.fxml uses btnProductOnActions with 's')
    @FXML
    public void btnProductOnActions(ActionEvent actionEvent) {
        btnProductOnAction(actionEvent);
    }
    
    // Alias for FXML compatibility (DashboardForm.fxml uses btnReturnsOrderOnAction)
    @FXML
    public void btnReturnsOrderOnAction(ActionEvent actionEvent) {
        btnReturnsOnAction(actionEvent);
    }
    
    // Alias for FXML compatibility (DashboardForm.fxml uses btnIncomeReportOnAction)
    @FXML
    public void btnIncomeReportOnAction(ActionEvent actionEvent) {
        btnReportsOnAction(actionEvent);
    }
    //sfshef
    /**
     * Navigate to Return Orders page
     * This method is no longer needed as we use btnReturnsOrderOnAction from BaseController
     * Kept for backward compatibility
     */
    @FXML
    public void btnPurchaseReturnOnAction(ActionEvent actionEvent) {
        btnReturnsOrderOnAction(actionEvent);
    }

    @FXML
    public void btnStockValuationOnAction(ActionEvent actionEvent) {
        // Navigate to Reports page
        btnIncomeReportOnAction(actionEvent);
    }
    
    private void loadWelcomeMessage() {
        try {
            String userName = UserSessionData.email != null && !UserSessionData.email.isEmpty() 
                ? UserSessionData.email.split("@")[0] : "User";
            String role = UserSessionData.userRole != null && !UserSessionData.userRole.isEmpty()
                ? UserSessionData.userRole.replace("ROLE_", "") : "User";
            
            LocalDateTime now = LocalDateTime.now();
            int hour = now.getHour();
            String shift = "08:00 - 17:00"; // Default shift

            
            String welcomeText = String.format("Welcome back, %s ",
                userName, role, shift);
            
            if (lblWelcomeMessage != null) {
                lblWelcomeMessage.setText(welcomeText);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void loadKpis() {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);
            
            // Today's revenue and orders
            Double todayRevenue = orderDetailService.getRevenueByDateRange(startOfDay, endOfDay);
            Long todayOrders = orderDetailService.countOrdersByDateRange(startOfDay, endOfDay);
            
            // Subtract today's refunds from revenue (net revenue)
            Double todayRefundAmount = returnOrderService.getTotalRefundAmountByDateRange(startOfDay, endOfDay);
            Double todayNetRevenue = (todayRevenue != null ? todayRevenue : 0.0) - (todayRefundAmount != null ? todayRefundAmount : 0.0);
            
            // Yesterday's revenue for comparison
            LocalDate yesterday = today.minusDays(1);
            LocalDateTime yesterdayStart = yesterday.atStartOfDay();
            LocalDateTime yesterdayEnd = yesterday.atTime(23, 59, 59);
            Double yesterdayRevenue = orderDetailService.getRevenueByDateRange(yesterdayStart, yesterdayEnd);
            
            // Subtract yesterday's refunds from revenue
            Double yesterdayRefundAmount = returnOrderService.getTotalRefundAmountByDateRange(yesterdayStart, yesterdayEnd);
            Double yesterdayNetRevenue = (yesterdayRevenue != null ? yesterdayRevenue : 0.0) - (yesterdayRefundAmount != null ? yesterdayRefundAmount : 0.0);
            
            // Last week's revenue for profit comparison
            LocalDate lastWeekStart = today.minusDays(7);
            LocalDate lastWeekEnd = today.minusDays(1);
            LocalDateTime lastWeekStartTime = lastWeekStart.atStartOfDay();
            LocalDateTime lastWeekEndTime = lastWeekEnd.atTime(23, 59, 59);
            Double lastWeekRevenue = orderDetailService.getRevenueByDateRange(lastWeekStartTime, lastWeekEndTime);
            
            // This week's revenue
            LocalDate thisWeekStart = today.minusDays(7);
            LocalDateTime thisWeekStartTime = thisWeekStart.atStartOfDay();
            Double thisWeekRevenue = orderDetailService.getRevenueByDateRange(thisWeekStartTime, endOfDay);
            
            // Calculate percentage changes based on net revenue
            String revenueChangeText = "--";
            if (yesterdayNetRevenue != null && yesterdayNetRevenue > 0 && todayNetRevenue != null) {
                double change = ((todayNetRevenue - yesterdayNetRevenue) / yesterdayNetRevenue) * 100;
                String symbol = change >= 0 ? "▲" : "▼";
                String style = change >= 0 ? "kpi-change-positive" : "kpi-change-negative";
                revenueChangeText = String.format("%s %.1f%% vs Yesterday", symbol, Math.abs(change));
                if (lblRevenueChange != null) {
                    lblRevenueChange.setText(revenueChangeText);
                    lblRevenueChange.getStyleClass().removeAll("kpi-change-positive", "kpi-change-negative");
                    lblRevenueChange.getStyleClass().add(style);
                }
            } else if (lblRevenueChange != null) {
                lblRevenueChange.setText("No comparison data");
            }
            
            // Total customers
            int totalCustomers = 0;
            try {
                totalCustomers = customerService.findAllCustomers().size();
            } catch (Exception ignored) {}
            
            // Total products
            int totalProducts = 0;
            try {
                totalProducts = productService.findAllProducts().size();
            } catch (Exception ignored) {}
            
            // Format and set texts - use net revenue (revenue minus refunds)
            String revenueText = String.format("LKR %,.2f", todayNetRevenue != null ? todayNetRevenue : 0.0);
            String totalCustomersText = String.valueOf(totalCustomers);
            String totalProductsText = "Total Products: " + totalProducts;
            
            if (lblTodayRevenue != null) lblTodayRevenue.setText(revenueText);
            if (lblTotalCustomers != null) lblTotalCustomers.setText(totalCustomersText);
            if (lblTotalProducts != null) lblTotalProducts.setText(totalProductsText);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void loadTopSellingProductsChart() {
        try {
            if (barChartTopProducts == null) {
                return;
            }
            
            barChartTopProducts.getData().clear();
            barChartTopProducts.setTitle(null);
            
            // Get top selling products for current month
            LocalDate today = LocalDate.now();
            LocalDate firstDayOfMonth = today.withDayOfMonth(1);
            LocalDateTime startDate = firstDayOfMonth.atStartOfDay();
            LocalDateTime endDate = today.atTime(23, 59, 59);
            
            List<Object[]> topProducts = orderItemService.getTopSellingProductsByQuantity(startDate, endDate);
            
            if (topProducts.isEmpty()) {
                barChartTopProducts.setTitle("No sales data for current month");
                return;
            }
            
            // Get all return orders in the date range to calculate net sales
            List<ReturnOrder> returnOrdersInRange = returnOrderService.findAllReturnOrders().stream()
                .filter(ro -> ro.getReturnDate() != null && 
                       !ro.getReturnDate().isBefore(startDate) && 
                       !ro.getReturnDate().isAfter(endDate))
                .collect(Collectors.toList());
            
            // Create a map of product code to returned quantity
            Map<Integer, Long> returnedQuantitiesByProduct = new java.util.HashMap<>();
            for (ReturnOrder returnOrder : returnOrdersInRange) {
                List<ReturnOrderItem> returnItems = returnOrderItemService.findByReturnOrderId(returnOrder.getId());
                for (ReturnOrderItem returnItem : returnItems) {
                    Integer productCode = returnItem.getProductCode();
                    Long returnedQty = returnedQuantitiesByProduct.getOrDefault(productCode, 0L);
                    returnedQuantitiesByProduct.put(productCode, returnedQty + returnItem.getReturnQuantity());
                }
            }
            
            // Calculate net sales (sold - returned) for each product
            List<ProductNetSales> netSalesList = new ArrayList<>();
            for (Object[] product : topProducts) {
                Integer productCode = ((Number) product[0]).intValue();
                String productName = (String) product[1];
                if (productName == null || productName.isEmpty()) {
                    productName = "Product #" + productCode;
                }
                long soldQuantity = ((Number) product[2]).longValue();
                long returnedQuantity = returnedQuantitiesByProduct.getOrDefault(productCode, 0L);
                long netQuantity = soldQuantity - returnedQuantity;
                
                // Only include products with positive net sales
                if (netQuantity > 0) {
                    netSalesList.add(new ProductNetSales(productCode, productName, netQuantity));
                }
            }
            
            if (netSalesList.isEmpty()) {
                barChartTopProducts.setTitle("No net sales data for current month");
                return;
            }
            
            // Sort by net quantity descending and limit to top 20
            netSalesList.sort(Comparator.comparing(ProductNetSales::getNetQuantity).reversed());
            
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Units Sold");
            
            netSalesList.stream()
                .limit(20)
                .forEach(product -> series.getData().add(
                    new XYChart.Data<>(formatProductLabel(product.getProductName()), product.getNetQuantity())
                ));
            
            if (topProductsCategoryAxis != null) {
                topProductsCategoryAxis.setTickLabelRotation(0);
            }
            if (topProductsNumberAxis != null) {
                topProductsNumberAxis.setForceZeroInRange(true);
            }
            
            barChartTopProducts.getData().add(series);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error loading top selling products chart: " + ex.getMessage());
        }
    }
    
    // Helper class for net sales calculation
    private static class ProductNetSales {
        private final Integer productCode;
        private final String productName;
        private final Long netQuantity;
        
        public ProductNetSales(Integer productCode, String productName, Long netQuantity) {
            this.productCode = productCode;
            this.productName = productName;
            this.netQuantity = netQuantity;
        }
        
        public Integer getProductCode() { return productCode; }
        public String getProductName() { return productName; }
        public Long getNetQuantity() { return netQuantity; }
    }
    
    private String formatProductLabel(String productName) {
        if (productName == null || productName.isBlank()) {
            return "Unnamed";
        }
        String trimmed = productName.trim();
        int wrapLength = 16;
        if (trimmed.length() <= wrapLength) {
            return trimmed;
        }
        StringBuilder builder = new StringBuilder();
        int index = 0;
        int lines = 0;
        while (index < trimmed.length() && lines < 2) {
            int end = Math.min(index + wrapLength, trimmed.length());
            builder.append(trimmed, index, end);
            index = end;
            lines++;
            if (index < trimmed.length() && lines < 2) {
                builder.append("\n");
            }
        }
        if (index < trimmed.length()) {
            builder.append("...");
        }
        return builder.toString();
    }
    
    private void loadMonthlyIncomeChart() {
        try {
            if (barChartMonthlyIncome == null || categoryAxis == null || numberAxis == null) {
                return;
            }
            
            // Get current month data
            LocalDate today = LocalDate.now();
            LocalDate firstDayOfMonth = today.withDayOfMonth(1);
            LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
            
            // Clear existing data
            barChartMonthlyIncome.getData().clear();
            
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Daily Income");
            
            // Get income for each day of the month (net revenue: revenue minus refunds)
            for (LocalDate date = firstDayOfMonth; !date.isAfter(lastDayOfMonth); date = date.plusDays(1)) {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(23, 59, 59);
                
                Double dailyRevenue = orderDetailService.getRevenueByDateRange(startOfDay, endOfDay);
                Double dailyRefunds = returnOrderService.getTotalRefundAmountByDateRange(startOfDay, endOfDay);
                
                double netRevenue = (dailyRevenue != null ? dailyRevenue : 0.0) - (dailyRefunds != null ? dailyRefunds : 0.0);
                
                String dayLabel = date.format(DateTimeFormatter.ofPattern("MM/dd"));
                series.getData().add(new XYChart.Data<>(dayLabel, netRevenue));
            }
            
            barChartMonthlyIncome.getData().add(series);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error loading monthly income chart: " + ex.getMessage());
        }
    }
    
    private void loadRecentTransactions() {
        try {
            if (tblRecentTransactions == null) {
                return;
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
            
            // Create a list to hold transactions with their dates for sorting
            List<TransactionWithDate> allTransactions = new ArrayList<>();
            
            // Get recent orders
            List<OrderDetail> recentOrders = orderDetailService.findAllOrderDetails();
            for (OrderDetail order : recentOrders) {
                String customerName = order.getCustomerName();
                if (customerName == null || customerName.isEmpty()) {
                    customerName = "Guest";
                }
                
                String formattedDate = order.getIssuedDate().format(formatter);
                String formattedAmount = String.format("LKR %,.2f", order.getTotalCost());
                String formattedDiscount = order.getDiscount() > 0 
                    ? String.format("LKR %,.2f", order.getDiscount()) 
                    : "No discount";
                
                allTransactions.add(new TransactionWithDate(
                    new RecentTransactionTm(customerName, formattedAmount, formattedDiscount, formattedDate),
                    order.getIssuedDate()
                ));
            }
            
            // Get recent return orders
            List<ReturnOrder> recentReturns = returnOrderService.findAllReturnOrders();
            for (ReturnOrder returnOrder : recentReturns) {
                String customerName = returnOrder.getCustomerEmail();
                if (customerName == null || customerName.isEmpty()) {
                    customerName = "Guest";
                }
                
                LocalDateTime returnDate = returnOrder.getReturnDate() != null 
                    ? returnOrder.getReturnDate() 
                    : LocalDateTime.now();
                
                String formattedDate = returnDate.format(formatter);
                // Show return as negative amount with "RETURN" prefix
                String formattedAmount = String.format("RETURN: -LKR %,.2f", returnOrder.getRefundAmount());
                String formattedDiscount = returnOrder.getStatus(); // Use status instead of discount for returns
                
                allTransactions.add(new TransactionWithDate(
                    new RecentTransactionTm(customerName, formattedAmount, formattedDiscount, formattedDate),
                    returnDate
                ));
            }
            
            // Sort by date (most recent first) and limit to 10
            List<RecentTransactionTm> transactionList = allTransactions.stream()
                .sorted(Comparator.comparing(TransactionWithDate::getDate).reversed())
                .limit(10)
                .map(TransactionWithDate::getTransaction)
                .collect(Collectors.toList());
            
            tblRecentTransactions.setItems(FXCollections.observableArrayList(transactionList));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error loading recent transactions: " + ex.getMessage());
        }
    }
    
    // Helper class to sort transactions by date
    private static class TransactionWithDate {
        private final RecentTransactionTm transaction;
        private final LocalDateTime date;
        
        public TransactionWithDate(RecentTransactionTm transaction, LocalDateTime date) {
            this.transaction = transaction;
            this.date = date;
        }
        
        public RecentTransactionTm getTransaction() {
            return transaction;
        }
        
        public LocalDateTime getDate() {
            return date;
        }
    }
    
    private void loadQuickStats() {
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);
            
            // Today's orders
            Long todayOrders = orderDetailService.countOrdersByDateRange(startOfDay, endOfDay);
            
            // Yesterday's orders for comparison
            LocalDate yesterday = today.minusDays(1);
            LocalDateTime yesterdayStart = yesterday.atStartOfDay();
            LocalDateTime yesterdayEnd = yesterday.atTime(23, 59, 59);
            Long yesterdayOrders = orderDetailService.countOrdersByDateRange(yesterdayStart, yesterdayEnd);
            
            if (lblTodayOrders != null) {
                long ordersCount = todayOrders != null ? todayOrders : 0L;
                lblTodayOrders.setText(String.valueOf(ordersCount));
            }
            
            // Update orders change indicator - compare with yesterday
            if (lblOrdersChange != null) {
                long todayCount = todayOrders != null ? todayOrders : 0L;
                long yesterdayCount = yesterdayOrders != null ? yesterdayOrders : 0L;
                
                if (yesterdayCount > 0 && todayCount >= 0) {
                    double change = ((double)(todayCount - yesterdayCount) / yesterdayCount) * 100;
                    String symbol = change >= 0 ? "▲" : "▼";
                    String changeText = String.format("%s %.1f%% vs Yesterday", symbol, Math.abs(change));
                    lblOrdersChange.setText(changeText);
                } else if (todayCount > 0 && yesterdayCount == 0) {
                    lblOrdersChange.setText("▲ New orders today");
                } else {
                    lblOrdersChange.setText("--");
                }
            }
            
            // Average order value - calculate using net revenue (revenue minus refunds)
            Double todayRevenue = orderDetailService.getRevenueByDateRange(startOfDay, endOfDay);
            Double todayRefunds = returnOrderService.getTotalRefundAmountByDateRange(startOfDay, endOfDay);
            Double todayNetRevenue = (todayRevenue != null ? todayRevenue : 0.0) - (todayRefunds != null ? todayRefunds : 0.0);
            
            // Calculate average order value from net revenue
            Double avgOrderValue = null;
            if (todayOrders != null && todayOrders > 0) {
                avgOrderValue = todayNetRevenue / todayOrders;
            } else {
                // Fallback to service method if no orders today
                avgOrderValue = orderDetailService.getAverageOrderValueByDateRange(startOfDay, endOfDay);
            }
            
            if (lblAvgOrderValue != null) {
                lblAvgOrderValue.setText(String.format("LKR %,.2f", avgOrderValue != null ? avgOrderValue : 0.0));
            }
            
            // Total products count
            int totalProductsCount = 0;
            int activeProductsCount = 0;
            try {
                totalProductsCount = productService.findAllProducts().size();
                // Count active products (products with active batches)
                activeProductsCount = (int) productService.findAllProducts().stream()
                    .filter(p -> {
                        try {
                            return !productDetailService.findActiveBatchesByProductCode(p.getCode()).isEmpty();
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();
            } catch (Exception ignored) {}
            
            if (lblTotalProductsCount != null) {
                lblTotalProductsCount.setText(String.valueOf(totalProductsCount));
            }
            if (lblActiveProducts != null) {
                lblActiveProducts.setText(activeProductsCount + " active products");
            }
            
            // Low stock count
            int lowStockCount = 0;
            try {
                lowStockCount = (int) productDetailService.findAllProductDetails().stream()
                    .filter(ProductDetail::isLowStock)
                    .count();
            } catch (Exception ignored) {}
            
            if (lblLowStockCount != null) {
                lblLowStockCount.setText(String.valueOf(lowStockCount));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error loading quick stats: " + ex.getMessage());
        }
    }
    
    private void loadTopProduct() {
        try {
            // Find product with most batches (as proxy for top selling)
            List<Product> products = productService.findAllProducts();
            
            if (products.isEmpty()) {
                if (lblTopProductName != null) lblTopProductName.setText("No products available");
                if (lblTopProductStats != null) lblTopProductStats.setText("--");
                return;
            }
            
            // Find product with most active batches
            Product topProduct = products.stream()
                    .max(Comparator.comparingInt(p -> {
                        try {
                            return productDetailService.findActiveBatchesByProductCode(p.getCode()).size();
                        } catch (Exception e) {
                            return 0;
                        }
                    }))
                    .orElse(products.get(0));
            
            int activeBatches = productDetailService.findActiveBatchesByProductCode(topProduct.getCode()).size();
            int totalStock = productDetailService.getTotalStockForProduct(topProduct.getCode());
            
            String productName = topProduct.getDescription();
            if (topProduct.getCategory() != null) {
                productName += " (" + topProduct.getCategory().getName() + ")";
            }
            
            String stats = String.format("Active Batches: %d | Total Stock: %d units", activeBatches, totalStock);
            
            if (lblTopProductName != null) lblTopProductName.setText(productName);
            if (lblTopProductStats != null) lblTopProductStats.setText(stats);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void loadPendingTasks() {
        try {
            // Pending return orders
            Long pendingReturns = returnOrderService.countByStatus("PENDING");
            
            // Total refund amount for pending returns
            List<ReturnOrder> pendingReturnOrders = returnOrderService.findByStatus("PENDING");
            double totalPendingRefunds = pendingReturnOrders.stream()
                    .mapToDouble(ReturnOrder::getRefundAmount)
                    .sum();
            
            // Today's returns (all statuses)
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
            Long todayReturns = returnOrderService.countReturnsByDateRange(startOfDay, endOfDay);
            
            // Update UI
            if (lblPendingReturns != null) {
                lblPendingReturns.setText(String.valueOf(pendingReturns != null ? pendingReturns : 0L));
            }
            
            if (lblPendingRefunds != null) {
                lblPendingRefunds.setText(String.format("LKR %,.2f", totalPendingRefunds));
            }
            
            if (lblTodayReturns != null) {
                lblTodayReturns.setText(String.valueOf(todayReturns != null ? todayReturns : 0L));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error loading return orders data: " + ex.getMessage());
        }
    }
    
    private void loadPendingPayments() {
        try {
            // Get all pending payments
            List<OrderDetail> pendingPayments = orderDetailService.findPendingPayments();
            
            // Calculate total amount
            double totalAmount = pendingPayments.stream()
                    .mapToDouble(OrderDetail::getTotalCost)
                    .sum();
            
            // Count by payment method
            long creditCount = pendingPayments.stream()
                    .filter(order -> "CREDIT".equals(order.getPaymentMethod()))
                    .count();
            
            long chequeCount = pendingPayments.stream()
                    .filter(order -> "CHEQUE".equals(order.getPaymentMethod()))
                    .count();
            
            // Update UI
            if (lblPendingPaymentsCount != null) {
                lblPendingPaymentsCount.setText(String.valueOf(pendingPayments.size()));
            }
            
            if (lblPendingPaymentsAmount != null) {
                lblPendingPaymentsAmount.setText(String.format("LKR %,.2f", totalAmount));
            }
            
            if (lblPendingPaymentsDetails != null) {
                if (pendingPayments.isEmpty()) {
                    lblPendingPaymentsDetails.setText("No pending payments");
                } else {
                    StringBuilder details = new StringBuilder();
                    if (creditCount > 0) {
                        details.append(creditCount).append(" Credit");
                    }
                    if (chequeCount > 0) {
                        if (details.length() > 0) details.append(", ");
                        details.append(chequeCount).append(" Cheque");
                    }
                    if (details.length() == 0) {
                        details.append("Other payment methods");
                    }
                    lblPendingPaymentsDetails.setText(details.toString());
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error loading pending payments data: " + ex.getMessage());
        }
    }
    
    // Table Model for Recent Transactions
    public static class RecentTransactionTm {
        private String customerName;
        private String amount;
        private String discount;
        private String date;
        
        public RecentTransactionTm(String customerName, String amount, String discount, String date) {
            this.customerName = customerName;
            this.amount = amount;
            this.discount = discount;
            this.date = date;
        }
        
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        
        public String getDiscount() { return discount; }
        public void setDiscount(String discount) { this.discount = discount; }
        
        public         String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
    }
    
    @FXML
    public void btnSettingsOnAction(ActionEvent event) {
        if (AuthorizationUtil.canAccessSettings()) {
            navigateTo("SettingsForm", false);
        } else {
            AuthorizationUtil.showAdminOnlyAlert();
        }
    }
    
    /**
     * Setup user menu item actions programmatically
     * MenuItems don't support onAction in FXML, so we set them here
     */
    private void setupUserMenuActions() {
        if (menuItemSettings != null) {
            menuItemSettings.setOnAction(event -> btnSettingsOnAction(event));
        }
        if (menuItemLogout != null) {
            menuItemLogout.setOnAction(event -> btnLogoutOnAction(event));
        }
    }
}
