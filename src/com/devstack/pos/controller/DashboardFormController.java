package com.devstack.pos.controller;

import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.entity.ReturnOrder;
import com.devstack.pos.util.UserSessionData;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.OrderItemService;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.CustomerService;
import com.devstack.pos.service.ReturnOrderService;
import com.devstack.pos.service.ProductService;
import com.devstack.pos.service.CategoryService;
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
    private Text lblActiveInvoices;
    
    @FXML
    private Text lblCustomersSecondary;
    
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
    private Text lblWeekRevenue;
    
    @FXML
    private Text lblWeekOrders;
    
    @FXML
    private Text lblLowStockCount;
    
    @FXML
    public void initialize() {
        // Initialize sidebar with user info
        initializeSidebar();
        
        System.out.println("Dashboard loaded for user: " + UserSessionData.email + " with role: " + UserSessionData.userRole);
        
        // Load all dashboard data on initialization
        refreshDashboard();
    }
    
    /**
     * Manual refresh button handler
     * Standard approach: User can manually refresh dashboard data anytime
     */
    @FXML
    public void btnRefreshOnAction(ActionEvent event) {
        refreshDashboard();
    }
    
    /**
     * Refreshes all dashboard data
     * Called on initialization and when refresh button is clicked
     */
    private void refreshDashboard() {
        try {
            loadWelcomeMessage();
            loadKpis();
            loadTopSellingProductsChart();
            loadMonthlyIncomeChart();
            loadRecentTransactions();
            loadQuickStats();
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
            
            // Yesterday's revenue for comparison
            LocalDate yesterday = today.minusDays(1);
            LocalDateTime yesterdayStart = yesterday.atStartOfDay();
            LocalDateTime yesterdayEnd = yesterday.atTime(23, 59, 59);
            Double yesterdayRevenue = orderDetailService.getRevenueByDateRange(yesterdayStart, yesterdayEnd);
            
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
            
            // Calculate percentage changes
            String revenueChangeText = "--";
            if (yesterdayRevenue != null && yesterdayRevenue > 0 && todayRevenue != null) {
                double change = ((todayRevenue - yesterdayRevenue) / yesterdayRevenue) * 100;
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
            
            // Customers total
            int totalCustomers = 0;
            try {
                totalCustomers = customerService.findAllCustomers().size();
            } catch (Exception ignored) {}
            
            // Format and set texts
            String revenueText = String.format("LKR %,.2f", todayRevenue != null ? todayRevenue : 0.0);
            String invoicesText = String.valueOf(todayOrders != null ? todayOrders : 0L);
            String customersSecondaryText = "Total Customers: " + totalCustomers;
            
            if (lblTodayRevenue != null) lblTodayRevenue.setText(revenueText);
            if (lblActiveInvoices != null) lblActiveInvoices.setText(invoicesText);
            if (lblCustomersSecondary != null) lblCustomersSecondary.setText(customersSecondaryText);
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
            
            // Get income for each day of the month
            for (LocalDate date = firstDayOfMonth; !date.isAfter(lastDayOfMonth); date = date.plusDays(1)) {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(23, 59, 59);
                
                Double dailyRevenue = orderDetailService.getRevenueByDateRange(startOfDay, endOfDay);
                double revenue = dailyRevenue != null ? dailyRevenue : 0.0;
                
                String dayLabel = date.format(DateTimeFormatter.ofPattern("MM/dd"));
                series.getData().add(new XYChart.Data<>(dayLabel, revenue));
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
            
            // Get recent orders (last 10)
            List<OrderDetail> recentOrders = orderDetailService.findAllOrderDetails().stream()
                .sorted(Comparator.comparing(OrderDetail::getIssuedDate).reversed())
                .limit(10)
                .collect(Collectors.toList());
            
            ObservableList<RecentTransactionTm> transactionList = FXCollections.observableArrayList();
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
            
            for (OrderDetail order : recentOrders) {
                String customerName = order.getCustomerName();
                if (customerName == null || customerName.isEmpty()) {
                    customerName = "Guest";
                }
                
                String formattedDate = order.getIssuedDate().format(formatter);
                String formattedAmount = String.format("LKR %,.2f", order.getTotalCost());
                
                transactionList.add(new RecentTransactionTm(
                    String.valueOf(order.getCode()),
                    customerName,
                    formattedAmount,
                    formattedDate
                ));
            }
            
            tblRecentTransactions.setItems(transactionList);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error loading recent transactions: " + ex.getMessage());
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
            
            // Average order value
            Double avgOrderValue = orderDetailService.getAverageOrderValueByDateRange(startOfDay, endOfDay);
            if (lblAvgOrderValue != null) {
                lblAvgOrderValue.setText(String.format("LKR %,.2f", avgOrderValue != null ? avgOrderValue : 0.0));
            }
            
            // This week revenue
            LocalDate thisWeekStart = today.minusDays(7);
            LocalDateTime thisWeekStartTime = thisWeekStart.atStartOfDay();
            Double thisWeekRevenue = orderDetailService.getRevenueByDateRange(thisWeekStartTime, endOfDay);
            Long thisWeekOrders = orderDetailService.countOrdersByDateRange(thisWeekStartTime, endOfDay);
            
            if (lblWeekRevenue != null) {
                lblWeekRevenue.setText(String.format("LKR %,.2f", thisWeekRevenue != null ? thisWeekRevenue : 0.0));
            }
            if (lblWeekOrders != null) {
                lblWeekOrders.setText((thisWeekOrders != null ? thisWeekOrders : 0L) + " orders");
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
        private String orderId;
        private String customerName;
        private String amount;
        private String date;
        
        public RecentTransactionTm(String orderId, String customerName, String amount, String date) {
            this.orderId = orderId;
            this.customerName = customerName;
            this.amount = amount;
            this.date = date;
        }
        
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
    }
}
