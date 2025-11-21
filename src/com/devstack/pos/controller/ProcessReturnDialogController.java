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
import com.devstack.pos.service.ReturnOrderItemService;
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
    private TableColumn<ReturnItemTm, String> colOrderedQty; // Changed to String to display both original and remaining quantities
    
    @FXML
    private TableColumn<ReturnItemTm, Spinner<Double>> colReturnQty;
    
    @FXML
    private TableColumn<ReturnItemTm, Double> colUnitPrice;
    
    @FXML
    private TableColumn<ReturnItemTm, Double> colLineTotal;
    
    @FXML
    private TableColumn<ReturnItemTm, Double> colRefundAmount;
    
    private final OrderDetailService orderDetailService;
    private final OrderItemService orderItemService;
    private final ReturnOrderService returnOrderService;
    private final ReturnOrderItemService returnOrderItemService;
    
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
        
        // Ordered Quantity column - shows both original ordered quantity and remaining quantity
        colOrderedQty.setCellValueFactory(cellData -> {
            // Return a string property - actual value is set in the cell factory
            return new SimpleStringProperty("");
        });
        colOrderedQty.setCellFactory(column -> new TableCell<ReturnItemTm, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    ReturnItemTm returnItem = getTableRow().getItem();
                    // Get original ordered quantity (supports decimal quantities)
                    Double originalQty = returnItem.getOrderedQuantity();
                    // Calculate remaining quantity
                    Double remainingQty = calculateRemainingQuantity(returnItem);
                    
                    // Format original quantity (supports decimals)
                    String originalStr;
                    if (originalQty != null) {
                        if (originalQty == originalQty.intValue()) {
                            originalStr = String.valueOf(originalQty.intValue());
                        } else {
                            originalStr = String.format("%.2f", originalQty);
                        }
                    } else {
                        originalStr = "0";
                    }
                    
                    // Format remaining quantity (supports decimals)
                    String remainingStr;
                    if (remainingQty != null) {
                        if (remainingQty == remainingQty.intValue()) {
                            remainingStr = String.valueOf(remainingQty.intValue());
                        } else {
                            remainingStr = String.format("%.2f", remainingQty);
                        }
                    } else {
                        remainingStr = "0";
                    }
                    
                    // Display format: "Original (Remaining remaining)" or just "Original" if fully returned
                    if (remainingQty != null && remainingQty > 0 && remainingQty < originalQty) {
                        setText(originalStr + " (" + remainingStr + " remaining)");
                        setStyle("-fx-text-fill: #64748b;"); // Gray for partial returns
                    } else if (remainingQty != null && remainingQty == 0) {
                        setText(originalStr + " (0 remaining)");
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;"); // Red for fully returned
                    } else {
                        setText(originalStr);
                        setStyle("-fx-text-fill: #16a34a;"); // Green for no returns yet
                    }
                    setAlignment(Pos.CENTER);
                }
            }
        });
        
        // Return Quantity Spinner column - needs custom cell factory (supports decimal quantities)
        colReturnQty.setCellValueFactory(cellData -> {
            ReturnItemTm item = cellData.getValue();
            return new javafx.beans.property.SimpleObjectProperty<>(item.getReturnQuantitySpinner());
        });
        colReturnQty.setCellFactory(column -> new TableCell<ReturnItemTm, Spinner<Double>>() {
            @Override
            protected void updateItem(Spinner<Double> spinner, boolean empty) {
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
                // Calculate initial value (supports decimal quantities)
                if (item.getSelectCheckBox().isSelected()) {
                    Double returnQty = item.getReturnQuantitySpinner().getValue();
                    if (returnQty != null && item.getUnitPrice() != null) {
                        refundProperty.set(returnQty * item.getUnitPrice());
                    }
                }
                
                // Listen to checkbox changes
                item.getSelectCheckBox().selectedProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal && item.getReturnQuantitySpinner().getValue() != null) {
                        Double returnQty = item.getReturnQuantitySpinner().getValue();
                        refundProperty.set(returnQty * item.getUnitPrice());
                    } else {
                        refundProperty.set(0.0);
                    }
                });
                
                // Listen to spinner changes (supports decimal quantities)
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
                
                // Count items that can still be returned
                long returnableItems = returnItems.stream()
                    .filter(item -> {
                        Double remaining = calculateRemainingQuantity(item);
                        return remaining != null && remaining > 0;
                    })
                    .count();
                
                String message;
                if (returnItems.isEmpty()) {
                    message = "Order loaded but has no items to display.";
                } else if (returnableItems == 0) {
                    message = "Order loaded successfully with " + returnItems.size() + 
                        " items.\n\nAll items have been fully returned. " +
                        "Items are shown but cannot be selected for return.";
                } else if (returnableItems < returnItems.size()) {
                    message = "Order loaded successfully with " + returnItems.size() + 
                        " items.\n\n" + returnableItems + " item(s) can still be returned. " +
                        (returnItems.size() - returnableItems) + " item(s) are fully returned.";
                } else {
                    message = "Order loaded successfully with " + returnItems.size() + 
                        " items!";
                }
                
                showAlert("Success", message, Alert.AlertType.INFORMATION);
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
                // Calculate remaining quantity (original - already returned) - supports decimal quantities
                List<ReturnOrderItem> previousReturns = returnOrderItemService.findByOrderItemId(orderItem.getId());
                double alreadyReturned = previousReturns.stream()
                    .mapToDouble(item -> item.getReturnQuantity() != null ? item.getReturnQuantity() : 0.0)
                    .sum();
                Double originalQty = orderItem.getQuantity();
                double remainingQty = (originalQty != null ? originalQty : 0.0) - alreadyReturned;
                
                // Show all items, even if fully returned (for visibility)
                boolean isFullyReturned = remainingQty <= 0;
                
                // Create checkbox
                CheckBox selectCheckBox = new CheckBox();
                selectCheckBox.setSelected(false);
                // Disable checkbox if item is fully returned
                selectCheckBox.setDisable(isFullyReturned);
                if (isFullyReturned) {
                    selectCheckBox.setTooltip(new Tooltip("This item has been fully returned"));
                }
                
                // Create spinner for return quantity (supports decimal quantities for items like sand, pipes)
                Spinner<Double> returnQtySpinner = new Spinner<>();
                // Use remaining quantity as max (supports decimals), minimum 0.0
                double maxQty = Math.max(0.0, remainingQty); // Ensure non-negative
                SpinnerValueFactory<Double> valueFactory = 
                    new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, maxQty, 0.0, 0.1); // Step by 0.1 for decimals
                returnQtySpinner.setValueFactory(valueFactory);
                returnQtySpinner.setEditable(true);
                returnQtySpinner.setPrefWidth(100);
                // Disable spinner if item is fully returned
                returnQtySpinner.setDisable(true); // Will be enabled only when checkbox is selected (if not fully returned)
                
                // Add validation for decimal input in editable spinner
                returnQtySpinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal == null || newVal.isEmpty()) {
                        return; // Allow empty for user to type
                    }
                    // Allow decimal numbers (e.g., 2.5, 3.75, 10.5)
                    if (!newVal.matches("^\\d*\\.?\\d*$")) {
                        returnQtySpinner.getEditor().setText(oldVal);
                    } else {
                        try {
                            double value = Double.parseDouble(newVal);
                            if (value < 0) {
                                returnQtySpinner.getEditor().setText(oldVal);
                            } else if (value > maxQty) {
                                returnQtySpinner.getEditor().setText(String.format("%.2f", maxQty));
                            }
                        } catch (NumberFormatException e) {
                            // Invalid format, revert
                            returnQtySpinner.getEditor().setText(oldVal);
                        }
                    }
                });
                
                // Enable spinner only when checkbox is selected AND item is not fully returned
                returnQtySpinner.setDisable(true);
                selectCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    // Only enable if checkbox is selected and item is not fully returned
                    returnQtySpinner.setDisable(!newVal || isFullyReturned);
                    if (newVal && !isFullyReturned) {
                        returnQtySpinner.getValueFactory().setValue(maxQty); // Set to remaining quantity
                    } else {
                        returnQtySpinner.getValueFactory().setValue(0.0);
                    }
                    updateTotalRefund();
                    tblOrderItems.refresh();
                });
                
                // Update refund when spinner value changes
                returnQtySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                    // Validate that return quantity doesn't exceed remaining quantity
                    if (newVal != null && newVal > maxQty) {
                        returnQtySpinner.getValueFactory().setValue(maxQty);
                    }
                    updateTotalRefund();
                    tblOrderItems.refresh();
                });
                
                // Store original quantity (needed for calculations) - supports decimal quantities
                Double originalQtyDouble = originalQty != null ? originalQty : 0.0;
                ReturnItemTm returnItem = new ReturnItemTm(
                    orderItem.getId(),
                    orderItem.getProductCode(),
                    orderItem.getProductName(),
                    orderItem.getBatchCode(),
                    orderItem.getBatchNumber(),
                    originalQtyDouble, // Store original quantity as Double (supports decimals)
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
                Double returnQty = item.getReturnQuantitySpinner().getValue();
                if (returnQty != null && returnQty > 0) {
                    totalRefund += returnQty * item.getUnitPrice();
                }
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
        
        // Validate return quantities (supports decimal quantities)
        for (ReturnItemTm item : selectedItems) {
            Double returnQty = item.getReturnQuantitySpinner().getValue();
            if (returnQty == null || returnQty <= 0) {
                showAlert("Validation Error", 
                    "Please enter a valid return quantity for " + item.getProductName() + 
                    "\n\nQuantity must be greater than zero.", 
                    Alert.AlertType.WARNING);
                return;
            }
            
            // Compare with ordered quantity (convert to double for decimal comparison)
            double orderedQty = item.getOrderedQuantity().doubleValue();
            if (returnQty > orderedQty) {
                showAlert("Validation Error", 
                    String.format("Return quantity cannot exceed ordered quantity for %s\n\n" +
                        "Ordered: %.2f\n" +
                        "Returning: %.2f\n" +
                        "Please reduce the return quantity.", 
                        item.getProductName(), orderedQty, returnQty), 
                    Alert.AlertType.WARNING);
                return;
            }
        }
        
        try {
            // Calculate total refund (supports decimal quantities)
            double totalRefundAmount = selectedItems.stream()
                .mapToDouble(item -> {
                    Double returnQty = item.getReturnQuantitySpinner().getValue();
                    return (returnQty != null ? returnQty : 0.0) * item.getUnitPrice();
                })
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
            
            // Create return order items (supports decimal quantities for items like sand, pipes, metal)
            List<ReturnOrderItem> returnOrderItems = new ArrayList<>();
            for (ReturnItemTm item : selectedItems) {
                Double returnQtyDouble = item.getReturnQuantitySpinner().getValue();
                // Use exact decimal quantity (e.g., 2.5 kg, 3.75 meters) - no rounding
                double refundAmount = returnQtyDouble * item.getUnitPrice();
                
                ReturnOrderItem returnOrderItem = ReturnOrderItem.builder()
                    .orderItemId(item.getOrderItemId())
                    .productCode(item.getProductCode())
                    .productName(item.getProductName())
                    .batchCode(item.getBatchCode())
                    .batchNumber(item.getBatchNumber())
                    .originalQuantity(item.getOrderedQuantity())
                    .returnQuantity(returnQtyDouble) // Store as decimal (supports 2.5, 3.75, etc.)
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
    
    /**
     * Calculate remaining quantity for an order item (original - already returned)
     * Supports decimal quantities for display
     */
    private double calculateRemainingQuantity(ReturnItemTm item) {
        try {
            // Get all previous returns for this order item
            List<ReturnOrderItem> previousReturns = returnOrderItemService.findByOrderItemId(item.getOrderItemId());
            
            // Sum up all returned quantities (supports decimal quantities)
            double alreadyReturned = previousReturns.stream()
                .mapToDouble(roi -> roi.getReturnQuantity() != null ? roi.getReturnQuantity() : 0.0)
                .sum();
            
            // Calculate remaining (original - already returned)
            // item.getOrderedQuantity() contains the original ordered quantity (supports decimals)
            Double originalQty = item.getOrderedQuantity();
            double originalQtyValue = originalQty != null ? originalQty : 0.0;
            double remaining = originalQtyValue - alreadyReturned;
            
            return Math.max(0.0, remaining); // Ensure non-negative, return as double for decimal support
        } catch (Exception e) {
            e.printStackTrace();
            // If error, return the original quantity
            Double originalQty = item.getOrderedQuantity();
            return originalQty != null ? originalQty : 0.0;
        }
    }
}
