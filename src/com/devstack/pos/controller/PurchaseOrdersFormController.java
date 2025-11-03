package com.devstack.pos.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.devstack.pos.entity.PurchaseOrder;
import com.devstack.pos.service.PurchaseOrderService;
import com.devstack.pos.util.AuthorizationUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
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
public class PurchaseOrdersFormController extends BaseController {
    
    private final PurchaseOrderService purchaseOrderService;
    
    @FXML
    private JFXTextField txtSearch;
    
    @FXML
    private ComboBox<String> cmbStatus;
    
    @FXML
    private DatePicker dateFrom;
    
    @FXML
    private DatePicker dateTo;
    
    @FXML
    private Text lblTotalPO;
    
    @FXML
    private Text lblPendingPO;
    
    @FXML
    private Text lblApprovedPO;
    
    @FXML
    private Text lblTotalValue;
    
    @FXML
    private TableView<PurchaseOrderTm> tblPurchaseOrders;
    
    @FXML
    private TableColumn<PurchaseOrderTm, String> colPONumber;
    
    @FXML
    private TableColumn<PurchaseOrderTm, String> colSupplier;
    
    @FXML
    private TableColumn<PurchaseOrderTm, String> colOrderDate;
    
    @FXML
    private TableColumn<PurchaseOrderTm, String> colExpectedDate;
    
    @FXML
    private TableColumn<PurchaseOrderTm, Integer> colItems;
    
    @FXML
    private TableColumn<PurchaseOrderTm, Double> colTotalAmount;
    
    @FXML
    private TableColumn<PurchaseOrderTm, String> colStatus;
    
    @FXML
    private TableColumn<PurchaseOrderTm, HBox> colAction;
    
    @FXML
    public void initialize() {
        // Initialize sidebar
        initializeSidebar();
        
        // Authorization check: Purchase Orders accessible by ADMIN only
        if (!AuthorizationUtil.canAccessPurchaseOrders()) {
            AuthorizationUtil.showAdminOnlyAlert();
            btnBackToDashboard(null);
            return;
        }
        
        // Initialize date pickers
        dateFrom.setValue(LocalDate.now().minusMonths(1));
        dateTo.setValue(LocalDate.now());
        
        // Initialize combo box
        ObservableList<String> statusList = FXCollections.observableArrayList("All", "Pending", "Approved", "Received", "Cancelled");
        cmbStatus.setItems(statusList);
        cmbStatus.setValue("All");
        
        // Configure table columns
        colPONumber.setCellValueFactory(new PropertyValueFactory<>("poNumber"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        colOrderDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        colExpectedDate.setCellValueFactory(new PropertyValueFactory<>("expectedDate"));
        colItems.setCellValueFactory(new PropertyValueFactory<>("items"));
        colTotalAmount.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("actionButtons"));
        
        // Load data
        loadPurchaseOrders();
        loadStatistics();
    }
    
    @Override
    protected String getCurrentPageName() {
        return "Purchase Orders";
    }
    
    @FXML
    public void btnBackToDashboard(ActionEvent actionEvent) {
        btnDashboardOnAction(actionEvent);
    }
    
    @FXML
    public void btnRefresh(ActionEvent event) {
        loadPurchaseOrders();
        loadStatistics();
    }
    
    @FXML
    public void btnSearch(ActionEvent event) {
        // Implement search functionality
        loadPurchaseOrders();
    }
    
    @FXML
    public void btnCreatePO(ActionEvent event) {
        // Open dialog to create new purchase order
        System.out.println("Create New PO clicked");
    }
    
    private void loadPurchaseOrders() {
        try {
            ObservableList<PurchaseOrderTm> data = FXCollections.observableArrayList();
            
            // Get search parameters
            String searchText = txtSearch.getText().trim();
            String statusFilter = cmbStatus.getValue();
            LocalDate fromDate = dateFrom.getValue();
            LocalDate toDate = dateTo.getValue();
            
            // Convert dates to LocalDateTime
            LocalDateTime startDateTime = fromDate.atStartOfDay();
            LocalDateTime endDateTime = toDate.atTime(LocalTime.MAX);
            
            // Fetch purchase orders from database
            List<PurchaseOrder> purchaseOrders;
            if (searchText.isEmpty() && "All".equals(statusFilter)) {
                purchaseOrders = purchaseOrderService.findByOrderDateBetween(startDateTime, endDateTime);
            } else {
                // Search with filters
                purchaseOrders = purchaseOrderService.searchPurchaseOrders(
                    searchText, searchText, statusFilter, startDateTime, endDateTime);
            }
            
            // Convert to table model
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (PurchaseOrder po : purchaseOrders) {
                HBox actions = createActionButtons(po);
                
                String expectedDate = po.getExpectedDeliveryDate() != null 
                    ? po.getExpectedDeliveryDate().format(dateFormatter) 
                    : "N/A";
                
                data.add(new PurchaseOrderTm(
                    po.getPoNumber(),
                    po.getSupplierName(),
                    po.getOrderDate().format(dateFormatter),
                    expectedDate,
                    0, // Items count - would need separate table for PO items
                    po.getTotalAmount(),
                    po.getStatus(),
                    actions
                ));
            }
            
            tblPurchaseOrders.setItems(data);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load purchase orders: " + e.getMessage());
        }
    }
    
    private HBox createActionButtons(PurchaseOrder po) {
        HBox hbox = new HBox(5);
        
        JFXButton viewBtn = new JFXButton("View");
        viewBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-padding: 5 10;");
        viewBtn.setOnAction(e -> viewPurchaseOrder(po));
        
        JFXButton editBtn = new JFXButton("Edit");
        editBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-padding: 5 10;");
        editBtn.setOnAction(e -> editPurchaseOrder(po));
        
        hbox.getChildren().addAll(viewBtn, editBtn);
        
        if ("PENDING".equals(po.getStatus())) {
            JFXButton approveBtn = new JFXButton("Approve");
            approveBtn.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-padding: 5 10;");
            approveBtn.setOnAction(e -> approvePurchaseOrder(po));
            hbox.getChildren().add(approveBtn);
        } else if ("APPROVED".equals(po.getStatus())) {
            JFXButton receiveBtn = new JFXButton("Receive");
            receiveBtn.setStyle("-fx-background-color: #8B5CF6; -fx-text-fill: white; -fx-padding: 5 10;");
            receiveBtn.setOnAction(e -> receivePurchaseOrder(po));
            hbox.getChildren().add(receiveBtn);
        }
        
        return hbox;
    }
    
    private void viewPurchaseOrder(PurchaseOrder po) {
        // TODO: Implement view details dialog
        System.out.println("Viewing PO: " + po.getPoNumber());
    }
    
    private void editPurchaseOrder(PurchaseOrder po) {
        // TODO: Implement edit dialog
        System.out.println("Editing PO: " + po.getPoNumber());
    }
    
    private void approvePurchaseOrder(PurchaseOrder po) {
        try {
            purchaseOrderService.approvePurchaseOrder(po.getId(), "admin");
            loadPurchaseOrders();
            loadStatistics();
            System.out.println("PO approved: " + po.getPoNumber());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to approve PO: " + e.getMessage());
        }
    }
    
    private void receivePurchaseOrder(PurchaseOrder po) {
        try {
            purchaseOrderService.receivePurchaseOrder(po.getId());
            loadPurchaseOrders();
            loadStatistics();
            System.out.println("PO received: " + po.getPoNumber());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to receive PO: " + e.getMessage());
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
            Long totalPO = purchaseOrderService.countPurchasesByDateRange(startDateTime, endDateTime);
            Long pendingPO = purchaseOrderService.countByStatus("PENDING");
            Long approvedPO = purchaseOrderService.countByStatus("APPROVED");
            Double totalValue = purchaseOrderService.getTotalPurchaseAmountByDateRange(startDateTime, endDateTime);
            
            // Update UI
            lblTotalPO.setText(totalPO != null ? totalPO.toString() : "0");
            lblPendingPO.setText(pendingPO != null ? pendingPO.toString() : "0");
            lblApprovedPO.setText(approvedPO != null ? approvedPO.toString() : "0");
            lblTotalValue.setText(String.format("%.2f /=", totalValue != null ? totalValue : 0.0));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load statistics: " + e.getMessage());
            // Set default values on error
            lblTotalPO.setText("0");
            lblPendingPO.setText("0");
            lblApprovedPO.setText("0");
            lblTotalValue.setText("0.00 /=");
        }
    }
    
    // Navigation methods inherited from BaseController
    
    // Table Model Class
    public static class PurchaseOrderTm {
        private String poNumber;
        private String supplier;
        private String orderDate;
        private String expectedDate;
        private int items;
        private double totalAmount;
        private String status;
        private HBox actionButtons;
        
        public PurchaseOrderTm(String poNumber, String supplier, String orderDate, String expectedDate,
                             int items, double totalAmount, String status, HBox actionButtons) {
            this.poNumber = poNumber;
            this.supplier = supplier;
            this.orderDate = orderDate;
            this.expectedDate = expectedDate;
            this.items = items;
            this.totalAmount = totalAmount;
            this.status = status;
            this.actionButtons = actionButtons;
        }
        
        public String getPoNumber() { return poNumber; }
        public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
        
        public String getSupplier() { return supplier; }
        public void setSupplier(String supplier) { this.supplier = supplier; }
        
        public String getOrderDate() { return orderDate; }
        public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
        
        public String getExpectedDate() { return expectedDate; }
        public void setExpectedDate(String expectedDate) { this.expectedDate = expectedDate; }
        
        public int getItems() { return items; }
        public void setItems(int items) { this.items = items; }
        
        public double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public HBox getActionButtons() { return actionButtons; }
        public void setActionButtons(HBox actionButtons) { this.actionButtons = actionButtons; }
    }
}

