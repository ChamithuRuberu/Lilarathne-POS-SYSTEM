package com.devstack.pos.controller;

import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.OrderItem;
import com.devstack.pos.entity.ReturnOrder;
import com.devstack.pos.entity.ReturnOrderItem;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.OrderItemService;
import com.devstack.pos.service.ReturnOrderService;
import com.devstack.pos.util.UserSessionData;
import com.devstack.pos.view.tm.ReturnItemTm;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProcessReturnDialogController {
    
    @FXML
    public AnchorPane dialogContext;
    
    @FXML
    private JFXTextField txtOrderId;
    
    @FXML
    private JFXTextField txtCustomerName;
    
    @FXML
    private JFXTextField txtOrderAmount;
    
    @FXML
    private JFXTextField txtOrderDate;
    
    @FXML
    private JFXTextField txtOperator;
    
    @FXML
    private JFXTextField txtTotalRefund;
    
    @FXML
    private ComboBox<String> cmbReturnReason;
    
    @FXML
    private JFXTextArea txtNotes;
    
    @FXML
    private TableView<ReturnItemTm> tblOrderItems;
    
    @FXML
    private TableColumn<ReturnItemTm, CheckBox> colSelect;
    
    @FXML
    private TableColumn<ReturnItemTm, String> colProductName;
    
    @FXML
    private TableColumn<ReturnItemTm, String> colBatchNumber;
    
    @FXML
    private TableColumn<ReturnItemTm, Integer> colOrderedQty;
    
    @FXML
    private TableColumn<ReturnItemTm, Spinner<Integer>> colReturnQty;
    
    @FXML
    private TableColumn<ReturnItemTm, Double> colUnitPrice;
    
    @FXML
    private TableColumn<ReturnItemTm, Double> colLineTotal;
    
    @FXML
    private TableColumn<ReturnItemTm, Double> colRefundAmount;
    
    private final OrderDetailService orderDetailService;
    private final OrderItemService orderItemService;
    private final ReturnOrderService returnOrderService;
    
    private OrderDetail loadedOrder;
    private ObservableList<ReturnItemTm> returnItems = FXCollections.observableArrayList();
    
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
        
        // Configure table columns
        // CheckBox column - needs custom cell factory
        colSelect.setCellValueFactory(cellData -> {
            ReturnItemTm item = cellData.getValue();
            return new javafx.beans.property.SimpleObjectProperty<>(item.getSelectCheckBox());
        });
        colSelect.setCellFactory(column -> new TableCell<ReturnItemTm, CheckBox>() {
            @Override
            protected void updateItem(CheckBox checkBox, boolean empty) {
                super.updateItem(checkBox, empty);
                if (empty || checkBox == null) {
                    setGraphic(null);
                } else {
                    setGraphic(checkBox);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        
        // Product Name column
        colProductName.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getProductName()));
        
        // Batch Number column
        colBatchNumber.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getBatchNumber() != null ? 
                cellData.getValue().getBatchNumber() : "N/A"));
        
        // Ordered Quantity column
        colOrderedQty.setCellValueFactory(new PropertyValueFactory<>("orderedQuantity"));
        colOrderedQty.setCellFactory(column -> new TableCell<ReturnItemTm, Integer>() {
            @Override
            protected void updateItem(Integer qty, boolean empty) {
                super.updateItem(qty, empty);
                if (empty || qty == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(qty));
                    setAlignment(Pos.CENTER);
                }
            }
        });
        
        // Return Quantity Spinner column - needs custom cell factory
        colReturnQty.setCellValueFactory(cellData -> {
            ReturnItemTm item = cellData.getValue();
            return new javafx.beans.property.SimpleObjectProperty<>(item.getReturnQuantitySpinner());
        });
        colReturnQty.setCellFactory(column -> new TableCell<ReturnItemTm, Spinner<Integer>>() {
            @Override
            protected void updateItem(Spinner<Integer> spinner, boolean empty) {
                super.updateItem(spinner, empty);
                if (empty || spinner == null) {
                    setGraphic(null);
                } else {
                    setGraphic(spinner);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        
        // Unit Price column
        colUnitPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        
        // Line Total column
        colLineTotal.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        
        // Refund amount calculation - reactive to checkbox and spinner changes
        colRefundAmount.setCellValueFactory(cellData -> {
            ReturnItemTm item = cellData.getValue();
            SimpleDoubleProperty refundProperty = new SimpleDoubleProperty(0.0);
            
            if (item != null && item.getReturnQuantitySpinner() != null && item.getSelectCheckBox() != null) {
                // Calculate initial value
                if (item.getSelectCheckBox().isSelected()) {
                    Integer returnQty = item.getReturnQuantitySpinner().getValue();
                    if (returnQty != null && item.getUnitPrice() != null) {
                        refundProperty.set(returnQty * item.getUnitPrice());
                    }
                }
                
                // Listen to checkbox changes
                item.getSelectCheckBox().selectedProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal && item.getReturnQuantitySpinner().getValue() != null) {
                        refundProperty.set(item.getReturnQuantitySpinner().getValue() * item.getUnitPrice());
                    } else {
                        refundProperty.set(0.0);
                    }
                });
                
                // Listen to spinner changes
                item.getReturnQuantitySpinner().valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (item.getSelectCheckBox().isSelected() && newVal != null) {
                        refundProperty.set(newVal * item.getUnitPrice());
                    }
                });
            }
            
            return refundProperty.asObject();
        });
        
        // Format price columns
        colUnitPrice.setCellFactory(column -> new TableCell<ReturnItemTm, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f /=", price));
                }
            }
        });
        
        colLineTotal.setCellFactory(column -> new TableCell<ReturnItemTm, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f /=", price));
                }
            }
        });
        
        colRefundAmount.setCellFactory(column -> new TableCell<ReturnItemTm, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f /=", price));
                    setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                }
            }
        });
    }
    
    @FXML
    public void btnLoadOrder(ActionEvent event) {
        String orderIdText = txtOrderId.getText().trim();
        
        if (orderIdText.isEmpty()) {
            showAlert("Validation Error", "Please enter an Order ID", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            Long orderId = Long.parseLong(orderIdText);
            loadedOrder = orderDetailService.findOrderDetail(orderId);
            
            if (loadedOrder != null) {
                // Populate order details
                txtOrderAmount.setText(String.format("%.2f /=", loadedOrder.getTotalCost()));
                txtOrderDate.setText(loadedOrder.getIssuedDate().toLocalDate().toString());
                txtCustomerName.setText(loadedOrder.getCustomerName() != null ? 
                    loadedOrder.getCustomerName() : "Guest");
                txtOperator.setText(loadedOrder.getOperatorEmail() != null ? 
                    loadedOrder.getOperatorEmail() : "N/A");
                
                // Load order items
                loadOrderItems(orderId);
                
                showAlert("Success", "Order loaded successfully with " + returnItems.size() + 
                    " items!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Not Found", "Order ID " + orderId + " not found in the system.", 
                    Alert.AlertType.ERROR);
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
    
    private void loadOrderItems(Long orderId) {
        returnItems.clear();
        tblOrderItems.setItems(returnItems); // Clear table first
        
        try {
            List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
            
            if (orderItems == null || orderItems.isEmpty()) {
                System.out.println("No order items found for order ID: " + orderId);
                showAlert("No Items", 
                    "This order has no items to return. " +
                    "Note: Only orders placed after the system update have item details.", 
                    Alert.AlertType.INFORMATION);
                return;
            }
            
            System.out.println("Found " + orderItems.size() + " items for order ID: " + orderId);
            
            for (OrderItem orderItem : orderItems) {
                // Create checkbox
                CheckBox selectCheckBox = new CheckBox();
                selectCheckBox.setSelected(false);
                
                // Create spinner for return quantity
                Spinner<Integer> returnQtySpinner = new Spinner<>();
                SpinnerValueFactory<Integer> valueFactory = 
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, orderItem.getQuantity(), 0);
                returnQtySpinner.setValueFactory(valueFactory);
                returnQtySpinner.setEditable(true);
                returnQtySpinner.setPrefWidth(80);
                
                // Enable spinner only when checkbox is selected
                returnQtySpinner.setDisable(true);
                selectCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    returnQtySpinner.setDisable(!newVal);
                    if (newVal) {
                        returnQtySpinner.getValueFactory().setValue(orderItem.getQuantity());
                    } else {
                        returnQtySpinner.getValueFactory().setValue(0);
                    }
                    updateTotalRefund();
                    tblOrderItems.refresh();
                });
                
                // Update refund when spinner value changes
                returnQtySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                    updateTotalRefund();
                    tblOrderItems.refresh();
                });
                
                ReturnItemTm returnItem = new ReturnItemTm(
                    orderItem.getId(),
                    orderItem.getProductCode(),
                    orderItem.getProductName(),
                    orderItem.getBatchCode(),
                    orderItem.getBatchNumber(),
                    orderItem.getQuantity(),
                    orderItem.getUnitPrice(),
                    orderItem.getLineTotal(),
                    selectCheckBox,
                    returnQtySpinner
                );
                
                returnItems.add(returnItem);
            }
            
            // Set items to table and refresh
            tblOrderItems.setItems(returnItems);
            tblOrderItems.refresh();
            
            System.out.println("Successfully loaded " + returnItems.size() + " items into table");
            
        } catch (Exception e) {
            System.err.println("Error loading order items: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to load order items: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    public void btnSelectAll(ActionEvent event) {
        for (ReturnItemTm item : returnItems) {
            item.getSelectCheckBox().setSelected(true);
        }
        updateTotalRefund();
    }
    
    @FXML
    public void btnDeselectAll(ActionEvent event) {
        for (ReturnItemTm item : returnItems) {
            item.getSelectCheckBox().setSelected(false);
        }
        updateTotalRefund();
    }
    
    private void updateTotalRefund() {
        double totalRefund = 0.0;
        
        for (ReturnItemTm item : returnItems) {
            if (item.getSelectCheckBox().isSelected() && item.getReturnQuantitySpinner() != null) {
                Integer returnQty = item.getReturnQuantitySpinner().getValue();
                totalRefund += returnQty * item.getUnitPrice();
            }
        }
        
        txtTotalRefund.setText(String.format("%.2f /=", totalRefund));
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
        
        if (cmbReturnReason.getValue() == null) {
            showAlert("Validation Error", "Please select a return reason", Alert.AlertType.WARNING);
            return;
        }
        
        // Check if at least one item is selected
        List<ReturnItemTm> selectedItems = returnItems.stream()
            .filter(item -> item.getSelectCheckBox().isSelected())
            .toList();
        
        if (selectedItems.isEmpty()) {
            showAlert("Validation Error", "Please select at least one item to return", 
                Alert.AlertType.WARNING);
            return;
        }
        
        // Validate return quantities
        for (ReturnItemTm item : selectedItems) {
            Integer returnQty = item.getReturnQuantitySpinner().getValue();
            if (returnQty == null || returnQty <= 0) {
                showAlert("Validation Error", 
                    "Please enter a valid return quantity for " + item.getProductName(), 
                    Alert.AlertType.WARNING);
                return;
            }
            if (returnQty > item.getOrderedQuantity()) {
                showAlert("Validation Error", 
                    "Return quantity cannot exceed ordered quantity for " + item.getProductName(), 
                    Alert.AlertType.WARNING);
                return;
            }
        }
        
        try {
            // Calculate total refund
            double totalRefundAmount = selectedItems.stream()
                .mapToDouble(item -> item.getReturnQuantitySpinner().getValue() * item.getUnitPrice())
                .sum();
            
            // Create return order
            ReturnOrder returnOrder = ReturnOrder.builder()
                .orderId(Math.toIntExact(loadedOrder.getCode()))
                .customerEmail(loadedOrder.getCustomerName() != null && !loadedOrder.getCustomerName().isEmpty() ? 
                    loadedOrder.getCustomerName() : "Guest")
                .originalAmount(loadedOrder.getTotalCost())
                .refundAmount(totalRefundAmount)
                .returnReason(cmbReturnReason.getValue())
                .notes(txtNotes.getText() != null && !txtNotes.getText().trim().isEmpty() ? 
                    txtNotes.getText().trim() : null)
                .status("PENDING")
                .processedBy(UserSessionData.email != null && !UserSessionData.email.isEmpty() ? 
                    UserSessionData.email : "System")
                .returnDate(LocalDateTime.now())
                .build();
            
            // Create return order items
            List<ReturnOrderItem> returnOrderItems = new ArrayList<>();
            for (ReturnItemTm item : selectedItems) {
                Integer returnQty = item.getReturnQuantitySpinner().getValue();
                double refundAmount = returnQty * item.getUnitPrice();
                
                ReturnOrderItem returnOrderItem = ReturnOrderItem.builder()
                    .orderItemId(item.getOrderItemId())
                    .productCode(item.getProductCode())
                    .productName(item.getProductName())
                    .batchCode(item.getBatchCode())
                    .batchNumber(item.getBatchNumber())
                    .originalQuantity(item.getOrderedQuantity())
                    .returnQuantity(returnQty)
                    .unitPrice(item.getUnitPrice())
                    .refundAmount(refundAmount)
                    .reason(cmbReturnReason.getValue())
                    .inventoryRestored(false)
                    .build();
                
                returnOrderItems.add(returnOrderItem);
            }
            
            // Save return order with items
            ReturnOrder savedReturn = returnOrderService.processReturnWithItems(returnOrder, returnOrderItems);
            
            showAlert("Success", 
                "Return processed successfully!\n\n" +
                "Return ID: " + savedReturn.getReturnId() + "\n" +
                "Order ID: " + txtOrderId.getText() + "\n" +
                "Items Returned: " + selectedItems.size() + "\n" +
                "Total Refund: " + String.format("%.2f /=", totalRefundAmount) + "\n" +
                "Status: " + savedReturn.getStatus() + "\n\n" +
                "Note: Inventory will be restored when return is completed.",
                Alert.AlertType.INFORMATION);
            
            // Close dialog
            closeDialog();
            
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
        txtCustomerName.clear();
        txtOrderAmount.clear();
        txtOrderDate.clear();
        txtOperator.clear();
        txtTotalRefund.clear();
        returnItems.clear();
        tblOrderItems.setItems(returnItems);
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
