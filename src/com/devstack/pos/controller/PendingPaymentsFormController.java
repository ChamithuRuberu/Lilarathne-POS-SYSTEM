package com.devstack.pos.controller;

import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.OrderItem;
import com.devstack.pos.entity.SuperAdminOrderDetail;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.OrderItemService;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.SuperAdminOrderDetailService;
import com.devstack.pos.util.AuthorizationUtil;
import com.devstack.pos.util.UserSessionData;
import com.devstack.pos.view.tm.PendingPaymentTm;
import com.jfoenix.controls.JFXButton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PendingPaymentsFormController extends BaseController {
    
    private final OrderDetailService orderDetailService;
    private final OrderItemService orderItemService;
    private final ProductDetailService productDetailService;
    private final SuperAdminOrderDetailService superAdminOrderDetailService;
    
    @FXML
    private Text lblTotalPending;
    
    @FXML
    private Text lblTotalAmount;
    
    @FXML
    private Text lblCreditOrders;
    
    @FXML
    private Text lblChequeOrders;
    
    @FXML
    private TableView<PendingPaymentTm> tblPendingPayments;
    
    @FXML
    private TableColumn<PendingPaymentTm, Long> colOrderId;
    
    @FXML
    private TableColumn<PendingPaymentTm, String> colCustomerName;
    
    @FXML
    private TableColumn<PendingPaymentTm, String> colDate;
    
    @FXML
    private TableColumn<PendingPaymentTm, String> colPaymentMethod;
    
    @FXML
    private TableColumn<PendingPaymentTm, String> colTotal;
    
    @FXML
    private TableColumn<PendingPaymentTm, String> colOperator;
    
    @FXML
    private TableColumn<PendingPaymentTm, JFXButton> colAction;
    
    private String currentFilter = "ALL";
    
    @FXML
    public void initialize() {
        // Initialize sidebar
        initializeSidebar();
        
        // Authorization check: Pending Payments accessible by ADMIN and CASHIER
        if (!AuthorizationUtil.canAccessPOSOrders()) {
            AuthorizationUtil.showUnauthorizedAlert();
            btnDashboardOnAction(null);
            return;
        }
        
        // Set table column resize policy
        tblPendingPayments.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Configure table columns
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("code"));
        colCustomerName.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateFormatted"));
        colPaymentMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalFormatted"));
        colOperator.setCellValueFactory(new PropertyValueFactory<>("operatorEmail"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("completePaymentButton"));
        
        // Load initial data
        loadPendingPayments();
    }
    
    @Override
    protected String getCurrentPageName() {
        return "Pending Payments";
    }
    
    @FXML
    public void btnBackToHomeOnAction(ActionEvent actionEvent) {
        btnDashboardOnAction(actionEvent);
    }
    
    @FXML
    public void btnRefreshOnAction(ActionEvent event) {
        loadPendingPayments();
    }
    
    @FXML
    public void btnFilterAll(ActionEvent event) {
        currentFilter = "ALL";
        loadPendingPayments();
    }
    
    @FXML
    public void btnFilterCredit(ActionEvent event) {
        currentFilter = "CREDIT";
        loadPendingPayments();
    }
    
    @FXML
    public void btnFilterCheque(ActionEvent event) {
        currentFilter = "CHEQUE";
        loadPendingPayments();
    }
    
    private void loadPendingPayments() {
        try {
            // Load regular pending orders - visible to ALL authorized users (ADMIN, CASHIER, and SUPER_ADMIN)
            List<OrderDetail> pendingOrders;
            
            // Get pending orders based on filter
            if ("CREDIT".equals(currentFilter)) {
                pendingOrders = orderDetailService.findPendingPaymentsByMethod("CREDIT");
            } else if ("CHEQUE".equals(currentFilter)) {
                pendingOrders = orderDetailService.findPendingPaymentsByMethod("CHEQUE");
            } else {
                pendingOrders = orderDetailService.findPendingPayments();
            }
            
            // Convert to table model
            ObservableList<PendingPaymentTm> observableList = FXCollections.observableArrayList();
            
            int creditCount = 0;
            int chequeCount = 0;
            double totalAmount = 0.0;
            
            // Add regular orders (visible to all authorized users including super admin)
            for (OrderDetail order : pendingOrders) {
                // Create complete payment button
                JFXButton completeBtn = new JFXButton("Complete Payment");
                completeBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 5;");
                completeBtn.setOnAction(e -> completePayment(order));
                
                PendingPaymentTm tm = new PendingPaymentTm();
                tm.setCode(order.getCode());
                tm.setCustomerName(order.getCustomerName() != null ? order.getCustomerName() : "Guest");
                tm.setIssuedDate(order.getIssuedDate());
                tm.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod() : "CASH");
                tm.setTotalCost(order.getTotalCost());
                tm.setOperatorEmail(order.getOperatorEmail());
                tm.setCompletePaymentButton(completeBtn);
                observableList.add(tm);
                
                // Update statistics
                totalAmount += order.getTotalCost();
                if ("CREDIT".equals(order.getPaymentMethod())) {
                    creditCount++;
                } else if ("CHEQUE".equals(order.getPaymentMethod())) {
                    chequeCount++;
                }
            }
            
            // Load super admin pending orders - ONLY visible to SUPER_ADMIN users
            List<SuperAdminOrderDetail> superAdminPendingOrders = null;
            if (UserSessionData.isSuperAdmin()) {
                // Get super admin pending orders based on filter
                if ("CREDIT".equals(currentFilter)) {
                    superAdminPendingOrders = superAdminOrderDetailService.findPendingPaymentsByMethod("CREDIT");
                } else if ("CHEQUE".equals(currentFilter)) {
                    superAdminPendingOrders = superAdminOrderDetailService.findPendingPaymentsByMethod("CHEQUE");
                } else {
                    superAdminPendingOrders = superAdminOrderDetailService.findPendingPayments();
                }
                
                // Add super admin orders (only visible to super admin)
                for (SuperAdminOrderDetail order : superAdminPendingOrders) {
                    // Create complete payment button
                    JFXButton completeBtn = new JFXButton("Complete Payment");
                    completeBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 5;");
                    completeBtn.setOnAction(e -> completeSuperAdminPayment(order));
                    
                    PendingPaymentTm tm = new PendingPaymentTm();
                    tm.setCode(order.getCode());
                    tm.setCustomerName(order.getCustomerName() != null ? order.getCustomerName() : "Guest");
                    tm.setIssuedDate(order.getIssuedDate());
                    tm.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod() : "CASH");
                    tm.setTotalCost(order.getTotalCost());
                    tm.setOperatorEmail(order.getOperatorEmail());
                    tm.setCompletePaymentButton(completeBtn);
                    observableList.add(tm);
                    
                    // Update statistics
                    totalAmount += order.getTotalCost();
                    if ("CREDIT".equals(order.getPaymentMethod())) {
                        creditCount++;
                    } else if ("CHEQUE".equals(order.getPaymentMethod())) {
                        chequeCount++;
                    }
                }
            }
            
            tblPendingPayments.setItems(observableList);
            
            // Update statistics labels
            int totalPendingCount = pendingOrders.size();
            if (superAdminPendingOrders != null) {
                totalPendingCount += superAdminPendingOrders.size();
            }
            
            lblTotalPending.setText(String.valueOf(totalPendingCount));
            lblTotalAmount.setText(String.format("LKR %.2f", totalAmount));
            lblCreditOrders.setText(String.valueOf(creditCount));
            lblChequeOrders.setText(String.valueOf(chequeCount));
            
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading pending payments: " + ex.getMessage()).show();
        }
    }
    
    private void completePayment(OrderDetail order) {
        try {
            // Confirm payment completion
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Complete Payment");
            confirmAlert.setHeaderText("Complete Payment for Order #" + order.getCode());
            confirmAlert.setContentText("Customer: " + order.getCustomerName() + "\n" +
                                       "Amount: LKR " + String.format("%.2f", order.getTotalCost()) + "\n" +
                                       "Payment Method: " + order.getPaymentMethod() + "\n\n" +
                                       "Are you sure you want to mark this payment as completed?");
            
            var result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                // Update payment status
                boolean success = orderDetailService.completePayment(order.getCode());
                
                if (success) {
                    // Get order items and reduce stock
                    List<OrderItem> orderItems = orderItemService.findByOrderId(order.getCode());
                    for (OrderItem item : orderItems) {
                        if (item.getBatchCode() != null && item.getQuantity() != null) {
                            // Use exact decimal quantity (supports 2.5, 3.75, etc.)
                            productDetailService.reduceStock(item.getBatchCode(), item.getQuantity());
                        }
                    }
                    
                    new Alert(Alert.AlertType.INFORMATION, 
                        "Payment completed successfully! Stock has been reduced.").show();
                    
                    // Reload pending payments
                    loadPendingPayments();
                } else {
                    new Alert(Alert.AlertType.ERROR, 
                        "Failed to complete payment. Order may have already been paid or does not exist.").show();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error completing payment: " + ex.getMessage()).show();
        }
    }
    
    private void completeSuperAdminPayment(SuperAdminOrderDetail order) {
        try {
            // Confirm payment completion
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Complete Payment");
            confirmAlert.setHeaderText("Complete Payment for Super Admin Order #" + order.getCode());
            confirmAlert.setContentText("Customer: " + order.getCustomerName() + "\n" +
                                       "Amount: LKR " + String.format("%.2f", order.getTotalCost()) + "\n" +
                                       "Payment Method: " + order.getPaymentMethod() + "\n\n" +
                                       "Are you sure you want to mark this payment as completed?");
            
            var result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                // Update payment status
                boolean success = superAdminOrderDetailService.completePayment(order.getCode());
                
                if (success) {
                    // Note: Super admin orders don't reduce stock (by design)
                    new Alert(Alert.AlertType.INFORMATION, 
                        "Super Admin payment completed successfully!").show();
                    
                    // Reload pending payments
                    loadPendingPayments();
                } else {
                    new Alert(Alert.AlertType.ERROR, 
                        "Failed to complete payment. Order may have already been paid or does not exist.").show();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error completing payment: " + ex.getMessage()).show();
        }
    }
}

