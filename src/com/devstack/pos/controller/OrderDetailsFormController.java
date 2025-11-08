package com.devstack.pos.controller;

import com.jfoenix.controls.JFXButton;
import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.service.OrderDetailService;
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
    public void initialize() {
        // Initialize sidebar
        initializeSidebar();
        
        // Fix table to prevent extra column
        tblOrders.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Initialize date pickers (default to last 30 days)
        dateFrom.setValue(LocalDate.now().minusDays(30));
        dateTo.setValue(LocalDate.now());
        
        // Configure table columns
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("code"));
        colCustomerName.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateFormatted"));
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("discountFormatted"));
        colOperator.setCellValueFactory(new PropertyValueFactory<>("operatorEmail"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalFormatted"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("viewButton"));
        
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
                
                OrderTm tm = new OrderTm();
                tm.setCode(order.getCode());
                tm.setCustomerName(order.getCustomerName() != null ? order.getCustomerName() : "Guest");
                tm.setIssuedDate(order.getIssuedDate());
                tm.setDiscount(order.getDiscount());
                tm.setOperatorEmail(order.getOperatorEmail());
                tm.setTotalCost(order.getTotalCost());
                tm.setViewButton(viewBtn);
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
            
            if (startDateTime != null && endDateTime != null) {
                totalOrders = orderDetailService.countOrdersByDateRange(startDateTime, endDateTime);
                totalRevenue = orderDetailService.getRevenueByDateRange(startDateTime, endDateTime);
                avgOrder = orderDetailService.getAverageOrderValueByDateRange(startDateTime, endDateTime);
                
                String period = fromDate.format(dateFormatter) + " to " + toDate.format(dateFormatter);
                lblOrdersPeriod.setText(period);
                lblRevenuePeriod.setText(period);
            } else {
                totalOrders = orderDetailService.getTotalOrderCount();
                totalRevenue = orderDetailService.getTotalRevenue();
                avgOrder = orderDetailService.getAverageOrderValue();
                
                lblOrdersPeriod.setText("All Time");
                lblRevenuePeriod.setText("All Time");
            }
            
            // Today's statistics
            LocalDate today = LocalDate.now();
            LocalDateTime todayStart = today.atStartOfDay();
            LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
            
            Long todayOrders = orderDetailService.countOrdersByDateRange(todayStart, todayEnd);
            Double todayRevenue = orderDetailService.getRevenueByDateRange(todayStart, todayEnd);
            
            // Update labels
            lblTotalOrders.setText(String.valueOf(totalOrders != null ? totalOrders : 0L));
            lblTotalRevenue.setText(String.format("LKR %,.2f", totalRevenue != null ? totalRevenue : 0.0));
            lblAvgOrder.setText(String.format("LKR %,.2f", avgOrder != null ? avgOrder : 0.0));
            lblTodayOrders.setText(String.valueOf(todayOrders != null ? todayOrders : 0L));
            lblTodayRevenue.setText(String.format("LKR %,.2f", todayRevenue != null ? todayRevenue : 0.0));
            
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
    
    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
