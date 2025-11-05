package com.devstack.pos.controller;

import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.ReturnOrderService;
import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.ReturnOrder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ProcessReturnDialogController {
    
    public AnchorPane dialogContext;
    
    @FXML
    private JFXTextField txtOrderId;
    
    @FXML
    private JFXTextField txtCustomerEmail;
    
    @FXML
    private JFXTextField txtOrderAmount;
    
    @FXML
    private JFXTextField txtOrderDate;
    
    @FXML
    private ComboBox<String> cmbReturnReason;
    
    @FXML
    private JFXTextField txtRefundAmount;
    
    @FXML
    private JFXTextArea txtNotes;
    
    private final OrderDetailService orderDetailService;
    private final ReturnOrderService returnOrderService;
    
    private OrderDetail loadedOrder;
    
    @FXML
    public void initialize() {
        // Populate return reasons
        ObservableList<String> reasons = FXCollections.observableArrayList(
            "Damaged Product",
            "Wrong Item Delivered",
            "Product Not as Described",
            "Quality Issues",
            "Customer Changed Mind",
            "Expired Product",
            "Defective Product",
            "Other"
        );
        cmbReturnReason.setItems(reasons);
    }
    
    @FXML
    public void btnLoadOrder(ActionEvent event) {
        String orderIdText = txtOrderId.getText().trim();
        
        if (orderIdText.isEmpty()) {
            showAlert("Validation Error", "Please enter an Order ID", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            Integer orderId = Integer.parseInt(orderIdText);
            loadedOrder = orderDetailService.findOrderDetail(orderId);
            
            if (loadedOrder != null) {
                // Populate order details
                txtOrderAmount.setText(String.format("%.2f", loadedOrder.getTotalCost()));
                txtOrderDate.setText(loadedOrder.getIssuedDate().toLocalDate().toString());
                txtRefundAmount.setText(String.format("%.2f", loadedOrder.getTotalCost()));
                
                showAlert("Success", "Order loaded successfully!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Not Found", "Order ID " + orderId + " not found in the system.", Alert.AlertType.ERROR);
                clearOrderFields();
                loadedOrder = null;
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter a valid numeric Order ID", Alert.AlertType.ERROR);
            loadedOrder = null;
        } catch (Exception e) {
            showAlert("Error", "Failed to load order: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
            loadedOrder = null;
        }
    }
    
    @FXML
    public void btnProcessReturn(ActionEvent event) {
        // Validate inputs
        if (txtOrderId.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter an Order ID", Alert.AlertType.WARNING);
            return;
        }
        
        if (loadedOrder == null) {
            showAlert("Validation Error", "Please load order details first", Alert.AlertType.WARNING);
            return;
        }
        
        if (txtCustomerEmail.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please load order details first", Alert.AlertType.WARNING);
            return;
        }
        
        if (cmbReturnReason.getValue() == null) {
            showAlert("Validation Error", "Please select a return reason", Alert.AlertType.WARNING);
            return;
        }
        
        if (txtRefundAmount.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter refund amount", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            double refundAmount = Double.parseDouble(txtRefundAmount.getText().trim());
            
            if (refundAmount <= 0) {
                showAlert("Validation Error", "Refund amount must be greater than 0", Alert.AlertType.WARNING);
                return;
            }
            
            // Create and save return order to database
            ReturnOrder returnOrder = ReturnOrder.builder()
                .orderId(Math.toIntExact(loadedOrder.getCode()))
                .customerEmail(loadedOrder.getCustomerName() != null ? loadedOrder.getCustomerName() : "Guest")
                .originalAmount(loadedOrder.getTotalCost())
                .refundAmount(refundAmount)
                .returnReason(cmbReturnReason.getValue())
                .notes(txtNotes.getText())
                .status("PENDING")
                .processedBy(loadedOrder.getOperatorEmail())
                .returnDate(LocalDateTime.now())
                .build();
            
            ReturnOrder savedReturn = returnOrderService.saveReturnOrder(returnOrder);
            
            showAlert("Success", 
                "Return processed successfully!\n\n" +
                "Return ID: " + savedReturn.getReturnId() + "\n" +
                "Order ID: " + txtOrderId.getText() + "\n" +
                "Refund Amount: " + String.format("%.2f /=", refundAmount) + "\n" +
                "Reason: " + cmbReturnReason.getValue() + "\n" +
                "Status: " + savedReturn.getStatus(),
                Alert.AlertType.INFORMATION);
            
            // Close dialog
            closeDialog();
            
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter a valid refund amount", Alert.AlertType.ERROR);
        } catch (Exception e) {
            showAlert("Error", "Failed to process return: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
    
    @FXML
    public void btnCancel(ActionEvent event) {
        closeDialog();
    }
    
    private void clearOrderFields() {
        txtCustomerEmail.clear();
        txtOrderAmount.clear();
        txtOrderDate.clear();
        txtRefundAmount.clear();
    }
    
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void closeDialog() {
        Stage stage = (Stage) dialogContext.getScene().getWindow();
        stage.close();
    }
}

