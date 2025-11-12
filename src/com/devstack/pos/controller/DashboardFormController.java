package com.devstack.pos.controller;

import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.entity.ReturnOrder;
import com.devstack.pos.util.AuthorizationUtil;
import com.devstack.pos.util.UserSessionData;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.OrderItemService;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.CustomerService;
import com.devstack.pos.service.ReturnOrderService;
import com.devstack.pos.service.ProductService;
import com.devstack.pos.service.CategoryService;
import com.jfoenix.controls.JFXButton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableView;
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
    private PieChart pieChartTopProducts;
    
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
    public void initialize() {
        // Initialize sidebar with user info
        initializeSidebar();
        
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
     * Admin users: All features including Products, Supplier
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
            
            if (hour >= 8 && hour < 17) {
                shift = "08:00 - 17:00";
            } else if (hour >= 17 && hour < 22) {
                shift = "17:00 - 22:00";
            } else {
                shift = "22:00 - 08:00";
            }
            
            String welcomeText = String.format("Welcome back, %s (%s) | Shift: %s", 
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
            if (pieChartTopProducts == null) {
                return;
            }
            
            // Get top selling products from last 30 days
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(30);
            
            List<Object[]> topProducts = orderItemService.getTopSellingProductsByQuantity(startDate, endDate);
            
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            
            if (topProducts.isEmpty()) {
                pieChartData.add(new PieChart.Data("No Sales Data", 100.0));
            } else {
                // Calculate total quantity for percentage
                long totalQuantity = topProducts.stream()
                    .mapToLong(item -> ((Number) item[2]).longValue())
                    .sum();
                
                // Add top 5 products to pie chart
                topProducts.stream()
                    .limit(5)
                    .forEach(item -> {
                        String productName = (String) item[1];
                        if (productName == null || productName.isEmpty()) {
                            productName = "Product #" + item[0];
                        }
                        long quantity = ((Number) item[2]).longValue();
                        double percentage = totalQuantity > 0 ? (quantity * 100.0 / totalQuantity) : 0.0;
                        pieChartData.add(new PieChart.Data(productName + " (" + quantity + ")", percentage));
                    });
            }
            
            pieChartTopProducts.setData(pieChartData);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error loading top selling products chart: " + ex.getMessage());
        }
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
            if (lblTodayOrders != null) {
                lblTodayOrders.setText(String.valueOf(todayOrders != null ? todayOrders : 0L));
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
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
    }
}
