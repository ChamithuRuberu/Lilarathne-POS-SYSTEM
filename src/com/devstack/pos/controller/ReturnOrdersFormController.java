package com.devstack.pos.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.devstack.pos.entity.ReturnOrder;
import com.devstack.pos.service.ReturnOrderService;
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
public class ReturnOrdersFormController {
    
    public AnchorPane context;
    
    private final ReturnOrderService returnOrderService;
    
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
    public void initialize() {
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
        colAction.setCellValueFactory(new PropertyValueFactory<>("actionButton"));
        
        // Load data
        loadReturnOrders();
        loadStatistics();
    }
    
    @FXML
    public void btnBackToDashboard(ActionEvent actionEvent) throws IOException {
        setUi("DashboardForm");
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
        // TODO: Implement view details dialog
        System.out.println("Viewing details for return: " + returnOrder.getReturnId());
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
    
    private void setUi(String url) throws IOException {
        Stage stage = (Stage) context.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/com/devstack/pos/view/" + url + ".fxml"));
        loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
        stage.setScene(new Scene(loader.load()));
        stage.centerOnScreen();
    }
    
    // Table Model Class
    public static class ReturnOrderTm {
        private int returnId;
        private int orderId;
        private String customer;
        private String returnDate;
        private String reason;
        private double amount;
        private String status;
        private JFXButton actionButton;
        
        public ReturnOrderTm(int returnId, int orderId, String customer, String returnDate, 
                           String reason, double amount, String status, JFXButton actionButton) {
            this.returnId = returnId;
            this.orderId = orderId;
            this.customer = customer;
            this.returnDate = returnDate;
            this.reason = reason;
            this.amount = amount;
            this.status = status;
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
        
        public JFXButton getActionButton() { return actionButton; }
        public void setActionButton(JFXButton actionButton) { this.actionButton = actionButton; }
    }
}

