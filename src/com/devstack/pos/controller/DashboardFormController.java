package com.devstack.pos.controller;

import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.entity.ReturnOrder;
import com.devstack.pos.util.UserSessionData;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.CustomerService;
import com.devstack.pos.service.ReturnOrderService;
import com.devstack.pos.service.ProductService;
import com.devstack.pos.service.CategoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DashboardFormController extends BaseController {
    
    private final OrderDetailService orderDetailService;
    private final ProductDetailService productDetailService;
    private final CustomerService customerService;
    private final ReturnOrderService returnOrderService;
    private final ProductService productService;
    private final CategoryService categoryService;
    
    // KPI Labels
    @FXML
    private Text lblTodayRevenue;
    
    @FXML
    private Text lblGrossProfit;
    
    @FXML
    private Text lblActiveInvoices;
    
    @FXML
    private Text lblCustomersSecondary;
    
    @FXML
    private Text lblCriticalStock;
    
    @FXML
    private Text lblNeedToReorder;
    
    @FXML
    private Text lblRevenueChange;
    
    @FXML
    private Text lblProfitChange;
    
    @FXML
    private Text lblWelcomeMessage;
    
    @FXML
    private PieChart pieChartSales;
    
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
            loadPieChart();
            loadTopProduct();
            loadPendingTasks();
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
            
            String profitChangeText = "--";
            if (lastWeekRevenue != null && lastWeekRevenue > 0 && thisWeekRevenue != null) {
                double change = ((thisWeekRevenue - lastWeekRevenue) / lastWeekRevenue) * 100;
                String symbol = change >= 0 ? "▲" : "▼";
                String style = change >= 0 ? "kpi-change-positive" : "kpi-change-negative";
                profitChangeText = String.format("%s %.1f%% vs Last Week", symbol, Math.abs(change));
                if (lblProfitChange != null) {
                    lblProfitChange.setText(profitChangeText);
                    lblProfitChange.getStyleClass().removeAll("kpi-change-positive", "kpi-change-negative");
                    lblProfitChange.getStyleClass().add(style);
                }
            } else if (lblProfitChange != null) {
                lblProfitChange.setText("No comparison data");
            }
            
            // Gross profit (not available - set to 0.00 for now)
            double grossProfit = 0.0;
            
            // Customers total
            int totalCustomers = 0;
            try {
                totalCustomers = customerService.findAllCustomers().size();
            } catch (Exception ignored) {}
            
            // Low stock counts
            int criticalStock = 0;
            try {
                criticalStock = (int) productDetailService.findAllProductDetails().stream()
                        .filter(ProductDetail::isLowStock)
                        .count();
            } catch (Exception ignored) {}
            
            int needToReorder = criticalStock; // same metric for now
            
            // Format and set texts
            String revenueText = String.format("LKR %,.2f", todayRevenue != null ? todayRevenue : 0.0);
            String profitText = String.format("LKR %,.2f", grossProfit);
            String invoicesText = String.valueOf(todayOrders != null ? todayOrders : 0L);
            String customersSecondaryText = "Total Customers: " + totalCustomers;
            String criticalStockText = (criticalStock) + " Items";
            String needToReorderText = "Need to Reorder: " + needToReorder;
            
            if (lblTodayRevenue != null) lblTodayRevenue.setText(revenueText);
            if (lblGrossProfit != null) lblGrossProfit.setText(profitText);
            if (lblActiveInvoices != null) lblActiveInvoices.setText(invoicesText);
            if (lblCustomersSecondary != null) lblCustomersSecondary.setText(customersSecondaryText);
            if (lblCriticalStock != null) lblCriticalStock.setText(criticalStockText);
            if (lblNeedToReorder != null) lblNeedToReorder.setText(needToReorderText);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void loadPieChart() {
        try {
            // Since we don't have order line items, we'll create a distribution based on categories
            // This is an approximation - showing category distribution of products
            List<com.devstack.pos.entity.Category> categories = categoryService.findAllCategories();
            
            if (categories.isEmpty() || pieChartSales == null) {
                return;
            }
            
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            
            // Get products by category and create distribution
            Map<String, Long> categoryCounts = productService.findAllProducts().stream()
                    .filter(p -> p.getCategory() != null)
                    .collect(Collectors.groupingBy(
                        p -> p.getCategory().getName(),
                        Collectors.counting()
                    ));
            
            if (categoryCounts.isEmpty()) {
                // If no category data, show a default message
                pieChartData.add(new PieChart.Data("No Data", 100.0));
            } else {
                // Calculate total for percentage
                long total = categoryCounts.values().stream().mapToLong(Long::longValue).sum();
                
                // Add pie chart data
                categoryCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5) // Top 5 categories
                    .forEach(entry -> {
                        double percentage = (entry.getValue().doubleValue() / total) * 100;
                        pieChartData.add(new PieChart.Data(entry.getKey(), percentage));
                    });
            }
            
            pieChartSales.setData(pieChartData);
        } catch (Exception ex) {
            ex.printStackTrace();
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
}
