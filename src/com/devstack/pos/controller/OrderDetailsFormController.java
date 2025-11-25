package com.devstack.pos.controller;

import com.jfoenix.controls.JFXButton;
import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.ReturnOrder;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.OrderItemService;
import com.devstack.pos.service.ReturnOrderService;
import com.devstack.pos.service.SuperAdminOrderDetailService;
import com.devstack.pos.service.SuperAdminOrderItemService;
import com.devstack.pos.util.UserSessionData;
import com.devstack.pos.view.tm.OrderTm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderDetailsFormController extends BaseController {
    
    private final OrderDetailService orderDetailService;
    private final OrderItemService orderItemService;
    private final ReturnOrderService returnOrderService;
    private final SuperAdminOrderDetailService superAdminOrderDetailService;
    private final SuperAdminOrderItemService superAdminOrderItemService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Summary Labels
    @FXML
    private Text lblTotalOrders;
    
    @FXML
    private Text lblTotalRevenue;
    
    @FXML
    private Text lblAvgOrder;
    
    @FXML
    private Text lblTodayOrders;
    
    @FXML
    private Text lblTodayRevenue;
    
    @FXML
    private Text lblOrdersPeriod;
    
    @FXML
    private Text lblRevenuePeriod;
    
    @FXML
    private Text lblRecordCount;
    
    // Filter Controls
    @FXML
    private com.jfoenix.controls.JFXTextField txtSearch;
    
    @FXML
    private DatePicker dateFrom;
    
    @FXML
    private DatePicker dateTo;
    
    // Table
    @FXML
    private TableView<OrderTm> tblOrders;
    
    @FXML
    private TableColumn<OrderTm, Long> colOrderId;
    
    @FXML
    private TableColumn<OrderTm, String> colCustomerName;
    
    @FXML
    private TableColumn<OrderTm, String> colProductNames;
    
    @FXML
    private TableColumn<OrderTm, String> colDate;
    
    @FXML
    private TableColumn<OrderTm, String> colDiscount;
    
    @FXML
    private TableColumn<OrderTm, String> colOperator;
    
    @FXML
    private TableColumn<OrderTm, String> colTotal;
    
    @FXML
    private TableColumn<OrderTm, String> colOrderType;
    
    @FXML
    private TableColumn<OrderTm, JFXButton> colAction;
    
    @FXML
    private TableColumn<OrderTm, JFXButton> colReturnOrders;
    
    @FXML
    public void initialize() {
        // Fix table to prevent extra column
        tblOrders.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Make table read-only (view-only)
        tblOrders.setEditable(false);
        
        // Initialize date pickers (default to last 30 days)
        dateFrom.setValue(LocalDate.now().minusDays(30));
        dateTo.setValue(LocalDate.now());
        
        // Configure table columns
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("code"));
        colOrderId.setEditable(false);
        colOrderId.setSortable(false);
        colCustomerName.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colCustomerName.setEditable(false);
        colCustomerName.setSortable(false);
        colProductNames.setCellValueFactory(new PropertyValueFactory<>("productNames"));
        colProductNames.setEditable(false);
        colProductNames.setSortable(false);
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateFormatted"));
        colDate.setEditable(false);
        colDate.setSortable(false);
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("discountFormatted"));
        colDiscount.setEditable(false);
        colDiscount.setSortable(false);
        colOperator.setCellValueFactory(new PropertyValueFactory<>("operatorEmail"));
        colOperator.setEditable(false);
        colOperator.setSortable(false);
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalFormatted"));
        colTotal.setEditable(false);
        colTotal.setSortable(false);
        colOrderType.setCellValueFactory(new PropertyValueFactory<>("orderTypeFormatted"));
        colOrderType.setEditable(false);
        colOrderType.setSortable(false);
        colAction.setCellValueFactory(new PropertyValueFactory<>("viewButton"));
        colAction.setEditable(false);
        colAction.setSortable(false);
        colReturnOrders.setCellValueFactory(new PropertyValueFactory<>("returnOrdersButton"));
        colReturnOrders.setEditable(false);
        colReturnOrders.setSortable(false);
        
        // Load initial data
        loadOrders();
        loadStatistics();
    }
    
    @Override
    protected String getCurrentPageName() {
        return "All Orders";
    }
    
    @FXML
    public void btnRefreshOnAction(ActionEvent event) {
        loadOrders();
        loadStatistics();
    }
    
    @FXML
    public void btnSearchOnAction(ActionEvent event) {
        loadOrders();
        loadStatistics();
    }
    
    @FXML
    public void btnClearOnAction(ActionEvent event) {
        txtSearch.clear();
        dateFrom.setValue(LocalDate.now().minusDays(30));
        dateTo.setValue(LocalDate.now());
        loadOrders();
        loadStatistics();
    }
    
    private void loadOrders() {
        try {
            // Get filter values
            String searchText = txtSearch.getText() != null ? txtSearch.getText().trim() : "";
            LocalDate fromDate = dateFrom.getValue();
            LocalDate toDate = dateTo.getValue();
            
            // Convert dates to LocalDateTime
            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;
            
            if (fromDate != null) {
                startDateTime = fromDate.atStartOfDay();
            }
            if (toDate != null) {
                endDateTime = toDate.atTime(LocalTime.MAX);
            }
            
            // Get orders based on filters (regular orders)
            List<OrderDetail> orders;
            if (startDateTime != null && endDateTime != null) {
                orders = orderDetailService.findOrdersByDateRange(startDateTime, endDateTime);
            } else {
                orders = orderDetailService.findAllOrderDetails();
            }
            
            // Apply search filter if provided
            if (!searchText.isEmpty()) {
                String searchLower = searchText.toLowerCase();
                orders = orders.stream()
                    .filter(order -> 
                        (order.getCode() != null && order.getCode().toString().contains(searchText)) ||
                        (order.getCustomerName() != null && order.getCustomerName().toLowerCase().contains(searchLower)) ||
                        (order.getOperatorEmail() != null && order.getOperatorEmail().toLowerCase().contains(searchLower))
                    )
                    .collect(Collectors.toList());
            }
            
            // For Super Admin: Get super admin orders using separate methods
            List<com.devstack.pos.entity.SuperAdminOrderDetail> superAdminOrders = new java.util.ArrayList<>();
            if (UserSessionData.isSuperAdmin()) {
                try {
                    if (startDateTime != null && endDateTime != null) {
                        superAdminOrders = superAdminOrderDetailService.findSuperAdminOrdersByDateRange(startDateTime, endDateTime);
                    } else {
                        superAdminOrders = superAdminOrderDetailService.findAllSuperAdminOrderDetails();
                    }
                    
                    // Apply search filter to super admin orders if provided
                    if (!searchText.isEmpty()) {
                        String searchLower = searchText.toLowerCase();
                        superAdminOrders = superAdminOrders.stream()
                            .filter(order -> 
                                (order.getCode() != null && order.getCode().toString().contains(searchText)) ||
                                (order.getCustomerName() != null && order.getCustomerName().toLowerCase().contains(searchLower)) ||
                                (order.getOperatorEmail() != null && order.getOperatorEmail().toLowerCase().contains(searchLower))
                            )
                            .collect(Collectors.toList());
                    }
                } catch (Exception e) {
                    System.err.println("Error loading super admin orders: " + e.getMessage());
                }
            }
            
            // Convert to table model
            ObservableList<OrderTm> observableList = FXCollections.observableArrayList();
            
            // Add regular orders
            for (OrderDetail order : orders) {
                // Create view button
                JFXButton viewBtn = new JFXButton("View");
                viewBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-background-radius: 5;");
                viewBtn.setOnAction(e -> viewOrderDetails(order));
                
                // Check for return orders
                List<ReturnOrder> returnOrders = returnOrderService.findByOrderId(order.getCode().intValue());
                JFXButton returnOrdersBtn = null;
                if (returnOrders != null && !returnOrders.isEmpty()) {
                    returnOrdersBtn = new JFXButton("View Returns (" + returnOrders.size() + ")");
                    returnOrdersBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-background-radius: 5;");
                    returnOrdersBtn.setOnAction(e -> viewReturnOrders(order.getCode().intValue(), returnOrders));
                } else {
                    returnOrdersBtn = new JFXButton("No Returns");
                    returnOrdersBtn.setStyle("-fx-background-color: #9CA3AF; -fx-text-fill: white; -fx-background-radius: 5;");
                    returnOrdersBtn.setDisable(true);
                }
                
                // Get product names for this order (supports decimal quantities)
                String productNames = orderItemService.findByOrderId(order.getCode())
                    .stream()
                    .map(item -> {
                        Double qty = item.getQuantity();
                        String qtyStr;
                        if (qty != null) {
                            if (qty == qty.intValue()) {
                                qtyStr = String.valueOf(qty.intValue());
                            } else {
                                qtyStr = String.format("%.2f", qty);
                            }
                        } else {
                            qtyStr = "0";
                        }
                        return item.getProductName() + " (x" + qtyStr + ")";
                    })
                    .collect(Collectors.joining(", "));
                
                if (productNames.isEmpty()) {
                    productNames = "No products";
                }
                
                OrderTm tm = new OrderTm();
                tm.setCode(order.getCode());
                tm.setCustomerName(order.getCustomerName() != null ? order.getCustomerName() : "Guest");
                tm.setProductNames(productNames);
                tm.setIssuedDate(order.getIssuedDate());
                tm.setDiscount(order.getDiscount());
                tm.setOperatorEmail(order.getOperatorEmail());
                tm.setTotalCost(order.getTotalCost());
                tm.setOrderType(order.getOrderType() != null ? order.getOrderType() : "HARDWARE");
                tm.setViewButton(viewBtn);
                tm.setReturnOrdersButton(returnOrdersBtn);
                observableList.add(tm);
            }
            
            // For Super Admin: Add super admin orders to the table
            for (com.devstack.pos.entity.SuperAdminOrderDetail order : superAdminOrders) {
                // Create view button for super admin order
                JFXButton viewBtn = new JFXButton("View");
                viewBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-background-radius: 5;");
                viewBtn.setOnAction(e -> viewSuperAdminOrderDetails(order));
                
                // Super admin orders don't have return orders (separate system)
                JFXButton returnOrdersBtn = new JFXButton("No Returns");
                returnOrdersBtn.setStyle("-fx-background-color: #9CA3AF; -fx-text-fill: white; -fx-background-radius: 5;");
                returnOrdersBtn.setDisable(true);
                
                // Get product names for this super admin order (including general items)
                String productNames = superAdminOrderItemService.findByOrderId(order.getCode())
                    .stream()
                    .map(item -> {
                        Double qty = item.getQuantity();
                        String qtyStr;
                        if (qty != null) {
                            if (qty == qty.intValue()) {
                                qtyStr = String.valueOf(qty.intValue());
                            } else {
                                qtyStr = String.format("%.2f", qty);
                            }
                        } else {
                            qtyStr = "0";
                        }
                        String itemName = item.getProductName();
                        // Mark general items
                        if (item.getIsGeneralItem() != null && item.getIsGeneralItem()) {
                            itemName = itemName + " [General Item]";
                        }
                        return itemName + " (x" + qtyStr + ")";
                    })
                    .collect(Collectors.joining(", "));
                
                if (productNames.isEmpty()) {
                    productNames = "No products";
                }
                
                OrderTm tm = new OrderTm();
                tm.setCode(order.getCode());
                tm.setCustomerName(order.getCustomerName() != null ? order.getCustomerName() : "Guest");
                tm.setProductNames(productNames);
                tm.setIssuedDate(order.getIssuedDate());
                tm.setDiscount(order.getDiscount());
                tm.setOperatorEmail(order.getOperatorEmail());
                tm.setTotalCost(order.getTotalCost());
                tm.setOrderType(order.getOrderType() != null ? order.getOrderType() : "HARDWARE");
                tm.setViewButton(viewBtn);
                tm.setReturnOrdersButton(returnOrdersBtn);
                observableList.add(tm);
            }
            
            // Sort by date (most recent first)
            observableList.sort((o1, o2) -> {
                if (o1.getIssuedDate() == null && o2.getIssuedDate() == null) return 0;
                if (o1.getIssuedDate() == null) return 1;
                if (o2.getIssuedDate() == null) return -1;
                return o2.getIssuedDate().compareTo(o1.getIssuedDate());
            });
            
            tblOrders.setItems(observableList);
            
            // Update record count (include super admin orders for super admin)
            int totalCount = orders.size() + (UserSessionData.isSuperAdmin() ? superAdminOrders.size() : 0);
            lblRecordCount.setText("Showing " + totalCount + " order(s)");
            
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error", "Failed to load orders: " + ex.getMessage());
        }
    }
    
    private void loadStatistics() {
        try {
            LocalDate fromDate = dateFrom.getValue();
            LocalDate toDate = dateTo.getValue();
            
            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;
            
            if (fromDate != null) {
                startDateTime = fromDate.atStartOfDay();
            }
            if (toDate != null) {
                endDateTime = toDate.atTime(LocalTime.MAX);
            }
            
            // Get statistics based on date range
            Long totalOrders;
            Double totalRevenue;
            Double avgOrder;
            Long totalReturnOrders;
            Double totalRefundAmount;
            
            if (startDateTime != null && endDateTime != null) {
                totalOrders = orderDetailService.countOrdersByDateRange(startDateTime, endDateTime);
                totalRevenue = orderDetailService.getRevenueByDateRange(startDateTime, endDateTime);
                avgOrder = orderDetailService.getAverageOrderValueByDateRange(startDateTime, endDateTime);
                totalReturnOrders = returnOrderService.countReturnsByDateRange(startDateTime, endDateTime);
                totalRefundAmount = returnOrderService.getTotalRefundAmountByDateRange(startDateTime, endDateTime);
                
                String period = fromDate.format(dateFormatter) + " to " + toDate.format(dateFormatter);
                lblOrdersPeriod.setText(period);
                lblRevenuePeriod.setText(period);
            } else {
                totalOrders = orderDetailService.getTotalOrderCount();
                totalRevenue = orderDetailService.getTotalRevenue();
                avgOrder = orderDetailService.getAverageOrderValue();
                totalReturnOrders = (long) returnOrderService.findAllReturnOrders().size();
                totalRefundAmount = returnOrderService.getTotalRefundAmount();
                
                lblOrdersPeriod.setText("All Time");
                lblRevenuePeriod.setText("All Time");
            }
            
            // Today's statistics
            LocalDate today = LocalDate.now();
            LocalDateTime todayStart = today.atStartOfDay();
            LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
            
            Long todayOrders = orderDetailService.countOrdersByDateRange(todayStart, todayEnd);
            Double todayRevenue = orderDetailService.getRevenueByDateRange(todayStart, todayEnd);
            Long todayReturnOrders = returnOrderService.countReturnsByDateRange(todayStart, todayEnd);
            Double todayRefundAmount = returnOrderService.getTotalRefundAmountByDateRange(todayStart, todayEnd);
            
            // Calculate net revenue (revenue - refunds)
            Double netRevenue = (totalRevenue != null ? totalRevenue : 0.0) - (totalRefundAmount != null ? totalRefundAmount : 0.0);
            Double todayNetRevenue = (todayRevenue != null ? todayRevenue : 0.0) - (todayRefundAmount != null ? todayRefundAmount : 0.0);
            
            // For Super Admin: Add super admin totals (which includes general items) to regular totals
            // For Regular Users: Show only regular totals (no general items)
            if (UserSessionData.isSuperAdmin()) {
                // Get super admin totals using separate methods
                Long superAdminOrders;
                Double superAdminRevenue;
                Long todaySuperAdminOrders;
                Double todaySuperAdminRevenue;
                
                if (startDateTime != null && endDateTime != null) {
                    superAdminOrders = superAdminOrderDetailService.countSuperAdminOrdersByDateRange(startDateTime, endDateTime);
                    superAdminRevenue = superAdminOrderDetailService.getSuperAdminRevenueByDateRange(startDateTime, endDateTime);
                } else {
                    superAdminOrders = superAdminOrderDetailService.getSuperAdminTotalOrderCount();
                    superAdminRevenue = superAdminOrderDetailService.getSuperAdminTotalRevenue();
                }
                todaySuperAdminOrders = superAdminOrderDetailService.countSuperAdminOrdersByDateRange(todayStart, todayEnd);
                todaySuperAdminRevenue = superAdminOrderDetailService.getSuperAdminRevenueByDateRange(todayStart, todayEnd);
                
                // Combine regular totals + super admin totals (which already includes general items)
                Long combinedOrders = (totalOrders != null ? totalOrders : 0L) + (superAdminOrders != null ? superAdminOrders : 0L);
                Double combinedRevenue = netRevenue + (superAdminRevenue != null ? superAdminRevenue : 0.0);
                Long combinedTodayOrders = (todayOrders != null ? todayOrders : 0L) + (todaySuperAdminOrders != null ? todaySuperAdminOrders : 0L);
                Double combinedTodayRevenue = todayNetRevenue + (todaySuperAdminRevenue != null ? todaySuperAdminRevenue : 0.0);
                
                // Recalculate average order value with combined totals
                Double combinedAvgOrder = (combinedOrders != null && combinedOrders > 0) 
                    ? (combinedRevenue / combinedOrders) : 0.0;
                
                // Display combined totals for super admin
                String ordersText = String.valueOf(combinedOrders);
                if (totalReturnOrders != null && totalReturnOrders > 0) {
                    ordersText += " (" + totalReturnOrders + " returns)";
                }
                lblTotalOrders.setText(ordersText);
                lblTotalRevenue.setText(String.format("LKR %,.2f", combinedRevenue));
                lblAvgOrder.setText(String.format("LKR %,.2f", combinedAvgOrder));
                
                String todayOrdersText = String.valueOf(combinedTodayOrders);
                if (todayReturnOrders != null && todayReturnOrders > 0) {
                    todayOrdersText += " (" + todayReturnOrders + " returns)";
                }
                lblTodayOrders.setText(todayOrdersText);
                lblTodayRevenue.setText(String.format("LKR %,.2f", combinedTodayRevenue));
                
                // Load detailed super admin totals
                loadSuperAdminTotals(startDateTime, endDateTime, todayStart, todayEnd);
            } else {
                // Regular users see only their totals (no general items)
            String ordersText = String.valueOf(totalOrders != null ? totalOrders : 0L);
            if (totalReturnOrders != null && totalReturnOrders > 0) {
                ordersText += " (" + totalReturnOrders + " returns)";
            }
            lblTotalOrders.setText(ordersText);
            lblTotalRevenue.setText(String.format("LKR %,.2f", netRevenue));
            lblAvgOrder.setText(String.format("LKR %,.2f", avgOrder != null ? avgOrder : 0.0));
            
            String todayOrdersText = String.valueOf(todayOrders != null ? todayOrders : 0L);
            if (todayReturnOrders != null && todayReturnOrders > 0) {
                todayOrdersText += " (" + todayReturnOrders + " returns)";
            }
            lblTodayOrders.setText(todayOrdersText);
            lblTodayRevenue.setText(String.format("LKR %,.2f", todayNetRevenue));
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error", "Failed to load statistics: " + ex.getMessage());
        }
    }
    
    /**
     * Load Super Admin totals using separate methods (not using existing methods)
     * Only visible to super admin users
     */
    private void loadSuperAdminTotals(LocalDateTime startDate, LocalDateTime endDate, LocalDateTime todayStart, LocalDateTime todayEnd) {
        try {
            // Get super admin order totals
            Double superAdminRevenue;
            Long superAdminOrders;
            Double superAdminAvgOrder;
            Double generalItemsRevenue;
            Double generalItemsQuantity;
            Long generalItemsOrderCount;
            
            if (startDate != null && endDate != null) {
                superAdminRevenue = superAdminOrderDetailService.getSuperAdminRevenueByDateRange(startDate, endDate);
                superAdminOrders = superAdminOrderDetailService.countSuperAdminOrdersByDateRange(startDate, endDate);
                superAdminAvgOrder = superAdminOrderDetailService.getSuperAdminAverageOrderValueByDateRange(startDate, endDate);
                generalItemsRevenue = superAdminOrderItemService.getGeneralItemsRevenueByDateRange(startDate, endDate);
                generalItemsQuantity = superAdminOrderItemService.getGeneralItemsQuantityByDateRange(startDate, endDate);
                generalItemsOrderCount = superAdminOrderItemService.getGeneralItemsOrderCountByDateRange(startDate, endDate);
            } else {
                superAdminRevenue = superAdminOrderDetailService.getSuperAdminTotalRevenue();
                superAdminOrders = superAdminOrderDetailService.getSuperAdminTotalOrderCount();
                superAdminAvgOrder = superAdminOrderDetailService.getSuperAdminAverageOrderValue();
                generalItemsRevenue = superAdminOrderItemService.getGeneralItemsTotalRevenue();
                generalItemsQuantity = superAdminOrderItemService.getGeneralItemsTotalQuantity();
                generalItemsOrderCount = superAdminOrderItemService.getGeneralItemsOrderCount();
            }
            
            // Get today's super admin totals
            Double todaySuperAdminRevenue = superAdminOrderDetailService.getSuperAdminRevenueByDateRange(todayStart, todayEnd);
            Long todaySuperAdminOrders = superAdminOrderDetailService.countSuperAdminOrdersByDateRange(todayStart, todayEnd);
            Double todayGeneralItemsRevenue = superAdminOrderItemService.getGeneralItemsRevenueByDateRange(todayStart, todayEnd);
            
            // Log super admin totals (you can add UI labels later)
            System.out.println("=== SUPER ADMIN TOTALS (Order Details) - Super Admin Only ===");
            System.out.println("Super Admin Revenue: " + String.format("%.2f /=", superAdminRevenue != null ? superAdminRevenue : 0.0));
            System.out.println("Super Admin Orders: " + (superAdminOrders != null ? superAdminOrders : 0));
            System.out.println("Super Admin Avg Order: " + String.format("%.2f /=", superAdminAvgOrder != null ? superAdminAvgOrder : 0.0));
            System.out.println("General Items Revenue: " + String.format("%.2f /=", generalItemsRevenue != null ? generalItemsRevenue : 0.0));
            System.out.println("General Items Quantity: " + String.format("%.2f", generalItemsQuantity != null ? generalItemsQuantity : 0.0));
            System.out.println("General Items Order Count: " + (generalItemsOrderCount != null ? generalItemsOrderCount : 0));
            System.out.println("Today Super Admin Revenue: " + String.format("%.2f /=", todaySuperAdminRevenue != null ? todaySuperAdminRevenue : 0.0));
            System.out.println("Today Super Admin Orders: " + (todaySuperAdminOrders != null ? todaySuperAdminOrders : 0));
            System.out.println("Today General Items Revenue: " + String.format("%.2f /=", todayGeneralItemsRevenue != null ? todayGeneralItemsRevenue : 0.0));
            System.out.println("=============================================================");
            
        } catch (Exception e) {
            System.err.println("Error loading super admin totals: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void viewOrderDetails(OrderDetail order) {
        try {
            // Get order items
            List<com.devstack.pos.entity.OrderItem> items = orderItemService.findByOrderId(order.getCode());
            
            StringBuilder details = new StringBuilder();
            details.append("Order Details\n\n");
            details.append(String.format("Order ID: %d\n", order.getCode()));
            details.append(String.format("Customer: %s\n", order.getCustomerName() != null ? order.getCustomerName() : "Guest"));
            details.append(String.format("Date: %s\n", order.getIssuedDate().format(dateTimeFormatter)));
            details.append(String.format("Discount: %.2f\n", order.getDiscount()));
            details.append(String.format("Total Amount: %.2f /=\n", order.getTotalCost()));
            details.append(String.format("Operator: %s\n", order.getOperatorEmail() != null ? order.getOperatorEmail() : "Unknown"));
            details.append(String.format("Payment Method: %s\n", order.getPaymentMethod() != null ? order.getPaymentMethod() : "CASH"));
            details.append(String.format("Payment Status: %s\n\n", order.getPaymentStatus() != null ? order.getPaymentStatus() : "PAID"));
            
            details.append("Items:\n");
            if (items.isEmpty()) {
                details.append("No items found.\n");
            } else {
                for (com.devstack.pos.entity.OrderItem item : items) {
                    details.append(String.format("- %s (x%.2f) @ %.2f = %.2f\n",
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()));
                }
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Order Details");
            alert.setHeaderText("Order #" + order.getCode());
            alert.setContentText(details.toString());
            alert.getDialogPane().setPrefWidth(500);
            alert.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error", "Failed to load order details: " + ex.getMessage());
        }
    }
    
    private void viewSuperAdminOrderDetails(com.devstack.pos.entity.SuperAdminOrderDetail order) {
        try {
            // Get super admin order items using separate methods
            List<com.devstack.pos.entity.SuperAdminOrderItem> items = superAdminOrderItemService.findByOrderId(order.getCode());
            
            StringBuilder details = new StringBuilder();
            details.append("Super Admin Order Details\n\n");
            details.append(String.format("Order ID: %d\n", order.getCode()));
            details.append(String.format("Customer: %s\n", order.getCustomerName() != null ? order.getCustomerName() : "Guest"));
            details.append(String.format("Date: %s\n", order.getIssuedDate().format(dateTimeFormatter)));
            details.append(String.format("Discount: %.2f\n", order.getDiscount()));
            details.append(String.format("Total Amount: %.2f /=\n", order.getTotalCost()));
            details.append(String.format("Operator: %s\n", order.getOperatorEmail() != null ? order.getOperatorEmail() : "Unknown"));
            details.append(String.format("Payment Method: %s\n", order.getPaymentMethod() != null ? order.getPaymentMethod() : "CASH"));
            details.append(String.format("Payment Status: %s\n\n", order.getPaymentStatus() != null ? order.getPaymentStatus() : "PAID"));
            
            details.append("Items:\n");
            if (items.isEmpty()) {
                details.append("No items found.\n");
            } else {
                for (com.devstack.pos.entity.SuperAdminOrderItem item : items) {
                    String itemName = item.getProductName();
                    if (item.getIsGeneralItem() != null && item.getIsGeneralItem()) {
                        itemName += " [General Item]";
                    }
                    details.append(String.format("- %s (x%.2f) @ %.2f = %.2f\n",
                        itemName,
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()));
                }
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Super Admin Order Details");
            alert.setHeaderText("Super Admin Order #" + order.getCode());
            alert.setContentText(details.toString());
            alert.getDialogPane().setPrefWidth(500);
            alert.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error", "Failed to load super admin order details: " + ex.getMessage());
        }
    }
    
    private void viewReturnOrders(Integer orderId, List<ReturnOrder> returnOrders) {
        try {
            StringBuilder details = new StringBuilder();
            details.append("Return Orders for Order #").append(orderId).append("\n\n");
            
            if (returnOrders.isEmpty()) {
                details.append("No return orders found for this order.");
            } else {
                for (ReturnOrder returnOrder : returnOrders) {
                    details.append("Return ID: ").append(returnOrder.getReturnId()).append("\n");
                    details.append("Status: ").append(returnOrder.getStatus()).append("\n");
                    details.append("Original Amount: LKR ").append(String.format("%.2f", returnOrder.getOriginalAmount())).append("\n");
                    details.append("Refund Amount: LKR ").append(String.format("%.2f", returnOrder.getRefundAmount())).append("\n");
                    details.append("Reason: ").append(returnOrder.getReturnReason()).append("\n");
                    if (returnOrder.getNotes() != null && !returnOrder.getNotes().isEmpty()) {
                        details.append("Notes: ").append(returnOrder.getNotes()).append("\n");
                    }
                    details.append("Return Date: ").append(returnOrder.getReturnDate().format(dateTimeFormatter)).append("\n");
                    if (returnOrder.getProcessedBy() != null) {
                        details.append("Processed By: ").append(returnOrder.getProcessedBy()).append("\n");
                    }
                    details.append("\n---\n\n");
                }
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Return Orders");
            alert.setHeaderText("Return Orders for Order #" + orderId);
            alert.setContentText(details.toString());
            alert.getDialogPane().setPrefWidth(500);
            alert.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error", "Failed to load return order details: " + ex.getMessage());
        }
    }
    
    @FXML
    public void btnBackToHomeOnAction(ActionEvent actionEvent) {
        btnDashboardOnAction(actionEvent);
    }
    
    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
