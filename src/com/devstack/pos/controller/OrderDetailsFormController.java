package com.devstack.pos.controller;

import com.jfoenix.controls.JFXButton;
import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.ReturnOrder;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.ReturnOrderService;
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
    private final ReturnOrderService returnOrderService;
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
    private TableColumn<OrderTm, String> colDate;
    
    @FXML
    private TableColumn<OrderTm, String> colDiscount;
    
    @FXML
    private TableColumn<OrderTm, String> colOperator;
    
    @FXML
    private TableColumn<OrderTm, String> colTotal;
    
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
            
            // Get orders based on filters
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
            
            // Convert to table model
            ObservableList<OrderTm> observableList = FXCollections.observableArrayList();
            
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
                
                OrderTm tm = new OrderTm();
                tm.setCode(order.getCode());
                tm.setCustomerName(order.getCustomerName() != null ? order.getCustomerName() : "Guest");
                tm.setIssuedDate(order.getIssuedDate());
                tm.setDiscount(order.getDiscount());
                tm.setOperatorEmail(order.getOperatorEmail());
                tm.setTotalCost(order.getTotalCost());
                tm.setViewButton(viewBtn);
                tm.setReturnOrdersButton(returnOrdersBtn);
                observableList.add(tm);
            }
            
            tblOrders.setItems(observableList);
            
            // Update record count
            lblRecordCount.setText("Showing " + orders.size() + " order(s)");
            
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
            
            // Update labels - include return order info in total orders
            String ordersText = String.valueOf(totalOrders != null ? totalOrders : 0L);
            if (totalReturnOrders != null && totalReturnOrders > 0) {
                ordersText += " (" + totalReturnOrders + " returns)";
            }
            lblTotalOrders.setText(ordersText);
            
            // Calculate net revenue (revenue - refunds)
            Double netRevenue = (totalRevenue != null ? totalRevenue : 0.0) - (totalRefundAmount != null ? totalRefundAmount : 0.0);
            lblTotalRevenue.setText(String.format("LKR %,.2f", netRevenue));
            
            lblAvgOrder.setText(String.format("LKR %,.2f", avgOrder != null ? avgOrder : 0.0));
            
            String todayOrdersText = String.valueOf(todayOrders != null ? todayOrders : 0L);
            if (todayReturnOrders != null && todayReturnOrders > 0) {
                todayOrdersText += " (" + todayReturnOrders + " returns)";
            }
            lblTodayOrders.setText(todayOrdersText);
            
            Double todayNetRevenue = (todayRevenue != null ? todayRevenue : 0.0) - (todayRefundAmount != null ? todayRefundAmount : 0.0);
            lblTodayRevenue.setText(String.format("LKR %,.2f", todayNetRevenue));
            
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error", "Failed to load statistics: " + ex.getMessage());
        }
    }
    
    private void viewOrderDetails(OrderDetail order) {
        try {
            String details = String.format(
                "Order Details\n\n" +
                "Order ID: %d\n" +
                "Customer: %s\n" +
                "Date: %s\n" +
                "Discount: %.2f\n" +
                "Total Amount: %.2f /=\n" +
                "Operator: %s",
                order.getCode(),
                order.getCustomerName() != null ? order.getCustomerName() : "Guest",
                order.getIssuedDate().format(dateTimeFormatter),
                order.getDiscount(),
                order.getTotalCost(),
                order.getOperatorEmail() != null ? order.getOperatorEmail() : "Unknown"
            );
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Order Details");
            alert.setHeaderText("Order #" + order.getCode());
            alert.setContentText(details);
            alert.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error", "Failed to load order details: " + ex.getMessage());
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
