package com.devstack.pos.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.devstack.pos.entity.ReturnOrder;
import com.devstack.pos.entity.ReturnOrderItem;
import com.devstack.pos.service.ReturnOrderItemService;
import com.devstack.pos.service.ReturnOrderService;
import com.devstack.pos.util.AuthorizationUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReturnOrdersFormController extends BaseController {
    
    private final ReturnOrderService returnOrderService;
    private final ReturnOrderItemService returnOrderItemService;
    
    @FXML
    private JFXTextField txtSearch;
    
    @FXML
    private DatePicker dateFrom;
    
    @FXML
    private DatePicker dateTo;
    
    @FXML
    private Text lblTotalReturns;
    
    @FXML
    private Text lblPendingReturns;
    
    @FXML
    private Text lblTotalRefund;
    
    @FXML
    private TableView<ReturnOrderTm> tblReturns;
    
    @FXML
    private TableColumn<ReturnOrderTm, Integer> colReturnId;
    
    @FXML
    private TableColumn<ReturnOrderTm, Integer> colOrderId;
    
    @FXML
    private TableColumn<ReturnOrderTm, String> colCustomer;
    
    @FXML
    private TableColumn<ReturnOrderTm, String> colReturnDate;
    
    @FXML
    private TableColumn<ReturnOrderTm, String> colReason;
    
    @FXML
    private TableColumn<ReturnOrderTm, Double> colAmount;
    
    @FXML
    private TableColumn<ReturnOrderTm, String> colStatus;
    
    @FXML
    private TableColumn<ReturnOrderTm, JFXButton> colAction;
    
    @FXML
    private TableColumn<ReturnOrderTm, String> colProcessedBy;
    
    @FXML
    public void initialize() {
        // Initialize sidebar
        initializeSidebar();
        
        // Authorization check: Return Orders accessible by ADMIN and CASHIER
        if (!AuthorizationUtil.canAccessReturnOrders()) {
            AuthorizationUtil.showUnauthorizedAlert();
            btnBackToDashboard(null);
            return;
        }
        
        // Set table column resize policy
        tblReturns.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Initialize date pickers
        dateFrom.setValue(LocalDate.now().minusMonths(1));
        dateTo.setValue(LocalDate.now());
        
        // Configure table columns
        colReturnId.setCellValueFactory(new PropertyValueFactory<>("returnId"));
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customer"));
        colReturnDate.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colProcessedBy.setCellValueFactory(new PropertyValueFactory<>("processedBy"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("actionButton"));
        
        // Load data
        loadReturnOrders();
        loadStatistics();
    }
    
    @Override
    protected String getCurrentPageName() {
        return "Return Orders";
    }
    
    @FXML
    public void btnBackToDashboard(ActionEvent actionEvent) {
        btnDashboardOnAction(actionEvent);
    }
    
    @FXML
    public void btnRefresh(ActionEvent event) {
        loadReturnOrders();
        loadStatistics();
    }
    
    @FXML
    public void btnSearch(ActionEvent event) {
        // Implement search functionality
        loadReturnOrders();
    }
    
    @FXML
    public void btnProcessReturn(ActionEvent event) {
        try {
            // Open dialog to process new return
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/devstack/pos/view/ProcessReturnDialog.fxml"));
            loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
            
            AnchorPane dialogPane = loader.load();
            
            // Create a new stage for the dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Process Return Order");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(context.getScene().getWindow());
            
            Scene scene = new Scene(dialogPane);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            
            dialogStage.showAndWait();
            
            // Refresh the table after dialog closes
            loadReturnOrders();
            loadStatistics();
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to open Process Return dialog: " + e.getMessage());
        }
    }
    
    private void loadReturnOrders() {
        try {
            ObservableList<ReturnOrderTm> data = FXCollections.observableArrayList();
            
            // Get search parameters
            String searchText = txtSearch.getText().trim();
            LocalDate fromDate = dateFrom.getValue();
            LocalDate toDate = dateTo.getValue();
            
            // Convert dates to LocalDateTime
            LocalDateTime startDateTime = fromDate.atStartOfDay();
            LocalDateTime endDateTime = toDate.atTime(LocalTime.MAX);
            
            // Fetch returns from database
            List<ReturnOrder> returnOrders;
            if (searchText.isEmpty()) {
                returnOrders = returnOrderService.findByReturnDateBetween(startDateTime, endDateTime);
            } else {
                // Search by return ID or customer email
                returnOrders = returnOrderService.searchReturnOrders(
                    searchText, searchText, "All", startDateTime, endDateTime);
            }
            
            // Convert to table model
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (ReturnOrder returnOrder : returnOrders) {
                JFXButton viewBtn = new JFXButton("View Details");
                viewBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white;");
                viewBtn.setOnAction(e -> viewReturnDetails(returnOrder));
                
                data.add(new ReturnOrderTm(
                    returnOrder.getId(),
                    returnOrder.getOrderId(),
                    returnOrder.getCustomerEmail(),
                    returnOrder.getReturnDate().format(dateFormatter),
                    returnOrder.getReturnReason(),
                    returnOrder.getRefundAmount(),
                    returnOrder.getStatus(),
                    returnOrder.getProcessedBy() != null ? returnOrder.getProcessedBy() : "N/A",
                    viewBtn
                ));
            }
            
            tblReturns.setItems(data);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load return orders: " + e.getMessage());
        }
    }
    
    private void viewReturnDetails(ReturnOrder returnOrder) {
        try {
            // Get return order items
            List<ReturnOrderItem> returnItems = returnOrderItemService.findByReturnOrderId(returnOrder.getId());
            
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Return Order Details");
            alert.setHeaderText("Return ID: " + (returnOrder.getReturnId() != null ? returnOrder.getReturnId() : returnOrder.getId()));
            
            StringBuilder content = new StringBuilder();
            content.append("═══════════════════════════════════════\n");
            content.append("ORDER INFORMATION\n");
            content.append("═══════════════════════════════════════\n");
            content.append("Order ID: ").append(returnOrder.getOrderId()).append("\n")
                   .append("Customer: ").append(returnOrder.getCustomerEmail()).append("\n")
                   .append("Original Amount: ").append(String.format("%.2f /=", returnOrder.getOriginalAmount())).append("\n")
                   .append("Refund Amount: ").append(String.format("%.2f /=", returnOrder.getRefundAmount())).append("\n")
                   .append("Reason: ").append(returnOrder.getReturnReason()).append("\n")
                   .append("Status: ").append(returnOrder.getStatus()).append("\n")
                   .append("Processed By: ").append(returnOrder.getProcessedBy() != null ? returnOrder.getProcessedBy() : "N/A").append("\n")
                   .append("Return Date: ").append(returnOrder.getReturnDate() != null ? 
                       returnOrder.getReturnDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A").append("\n");
            
            if (returnOrder.getNotes() != null && !returnOrder.getNotes().isEmpty()) {
                content.append("Notes: ").append(returnOrder.getNotes()).append("\n");
            }
            
            // Add product details
            if (!returnItems.isEmpty()) {
                content.append("\n═══════════════════════════════════════\n");
                content.append("RETURNED PRODUCTS (").append(returnItems.size()).append(" items)\n");
                content.append("═══════════════════════════════════════\n");
                
                int itemNumber = 1;
                for (ReturnOrderItem item : returnItems) {
                    content.append("\n").append(itemNumber++).append(". ").append(item.getProductName()).append("\n");
                    content.append("   Batch: ").append(item.getBatchNumber() != null ? item.getBatchNumber() : "N/A").append("\n");
                    content.append("   Return Qty: ").append(item.getReturnQuantity())
                           .append(" / ").append(item.getOriginalQuantity()).append(" (ordered)\n");
                    content.append("   Unit Price: ").append(String.format("%.2f /=", item.getUnitPrice())).append("\n");
                    content.append("   Refund: ").append(String.format("%.2f /=", item.getRefundAmount())).append("\n");
                    content.append("   Inventory Restored: ").append(item.getInventoryRestored() ? "Yes ✓" : "No ✗").append("\n");
                }
                
                content.append("\n═══════════════════════════════════════\n");
                content.append("Total Refund: ").append(String.format("%.2f /=", returnOrder.getRefundAmount())).append("\n");
                content.append("═══════════════════════════════════════\n");
            } else {
                content.append("\n⚠ No product details available for this return.\n");
            }
            
            alert.setContentText(content.toString());
            
            // Add action buttons for PENDING returns
            if ("PENDING".equals(returnOrder.getStatus())) {
                ButtonType approveButton = new ButtonType("Approve & Restore Inventory", ButtonBar.ButtonData.OK_DONE);
                ButtonType completeButton = new ButtonType("Complete Return", ButtonBar.ButtonData.APPLY);
                ButtonType rejectButton = new ButtonType("Reject", ButtonBar.ButtonData.NO);
                ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
                
                alert.getButtonTypes().setAll(approveButton, completeButton, rejectButton, closeButton);
                
                alert.showAndWait().ifPresent(response -> {
                    try {
                        if (response == approveButton) {
                            returnOrderService.approveReturn(returnOrder.getId(), "Current User");
                            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, 
                                "Return order approved successfully!").showAndWait();
                            loadReturnOrders();
                            loadStatistics();
                        } else if (response == completeButton) {
                            returnOrderService.completeReturn(returnOrder.getId());
                            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, 
                                "Return order completed and inventory restored successfully!").showAndWait();
                            loadReturnOrders();
                            loadStatistics();
                        } else if (response == rejectButton) {
                            returnOrderService.rejectReturn(returnOrder.getId(), "Current User");
                            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, 
                                "Return order rejected successfully!").showAndWait();
                            loadReturnOrders();
                            loadStatistics();
                        }
                    } catch (Exception e) {
                        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, 
                            "Error updating return: " + e.getMessage()).showAndWait();
                    }
                });
            } else {
                alert.showAndWait();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, 
                "Error loading return details: " + ex.getMessage()).showAndWait();
        }
    }
    
    private void loadStatistics() {
        try {
            // Get date range
            LocalDate fromDate = dateFrom.getValue();
            LocalDate toDate = dateTo.getValue();
            LocalDateTime startDateTime = fromDate.atStartOfDay();
            LocalDateTime endDateTime = toDate.atTime(LocalTime.MAX);
            
            // Fetch statistics from database
            Long totalReturns = returnOrderService.countReturnsByDateRange(startDateTime, endDateTime);
            Long pendingReturns = returnOrderService.countByStatus("PENDING");
            Double totalRefund = returnOrderService.getTotalRefundAmountByDateRange(startDateTime, endDateTime);
            
            // Update UI
            lblTotalReturns.setText(totalReturns != null ? totalReturns.toString() : "0");
            lblPendingReturns.setText(pendingReturns != null ? pendingReturns.toString() : "0");
            lblTotalRefund.setText(String.format("%.2f /=", totalRefund != null ? totalRefund : 0.0));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load statistics: " + e.getMessage());
            // Set default values on error
            lblTotalReturns.setText("0");
            lblPendingReturns.setText("0");
            lblTotalRefund.setText("0.00 /=");
        }
    }
    
    // Navigation methods inherited from BaseController
    
    // Table Model Class
    public static class ReturnOrderTm {
        private int returnId;
        private int orderId;
        private String customer;
        private String returnDate;
        private String reason;
        private double amount;
        private String status;
        private String processedBy;
        private JFXButton actionButton;
        
        public ReturnOrderTm(int returnId, int orderId, String customer, String returnDate, 
                           String reason, double amount, String status, String processedBy, JFXButton actionButton) {
            this.returnId = returnId;
            this.orderId = orderId;
            this.customer = customer;
            this.returnDate = returnDate;
            this.reason = reason;
            this.amount = amount;
            this.status = status;
            this.processedBy = processedBy;
            this.actionButton = actionButton;
        }
        
        public int getReturnId() { return returnId; }
        public void setReturnId(int returnId) { this.returnId = returnId; }
        
        public int getOrderId() { return orderId; }
        public void setOrderId(int orderId) { this.orderId = orderId; }
        
        public String getCustomer() { return customer; }
        public void setCustomer(String customer) { this.customer = customer; }
        
        public String getReturnDate() { return returnDate; }
        public void setReturnDate(String returnDate) { this.returnDate = returnDate; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getProcessedBy() { return processedBy; }
        public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
        
        public JFXButton getActionButton() { return actionButton; }
        public void setActionButton(JFXButton actionButton) { this.actionButton = actionButton; }
    }
}

