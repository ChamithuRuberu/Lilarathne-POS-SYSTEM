package com.devstack.pos.controller;

import com.devstack.pos.entity.GeneralItem;
import com.devstack.pos.service.GeneralItemService;
import com.devstack.pos.util.AuthorizationUtil;
import com.devstack.pos.util.UserSessionData;
import com.devstack.pos.view.tm.GeneralItemSelectedTm;
import com.devstack.pos.view.tm.GeneralItemTm;
import com.jfoenix.controls.JFXTextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GeneralItemsDialogController {
    
    @FXML
    public AnchorPane dialogContext;
    
    @FXML
    private TableView<GeneralItemTm> tblProducts;
    
    @FXML
    private TableColumn<GeneralItemTm, String> colProductName;
    
    @FXML
    private TableColumn<GeneralItemTm, CheckBox> colSelect;
    
    @FXML
    private TableView<GeneralItemSelectedTm> tblSelectedItems;
    
    @FXML
    private TableColumn<GeneralItemSelectedTm, String> colSelProductName;
    
    @FXML
    private TableColumn<GeneralItemSelectedTm, TextField> colSelQuantity;
    
    @FXML
    private TableColumn<GeneralItemSelectedTm, TextField> colSelUnitPrice;
    
    @FXML
    private TableColumn<GeneralItemSelectedTm, Double> colSelTotal;
    
    @FXML
    private TableColumn<GeneralItemSelectedTm, Button> colSelRemove;
    
    @FXML
    private JFXTextField txtTransportFee;
    
    @FXML
    private Text lblSubtotal;
    
    @FXML
    private Text lblTransportFee;
    
    @FXML
    private Text lblTotal;
    
    private final GeneralItemService generalItemService;
    private ObservableList<GeneralItemTm> productList = FXCollections.observableArrayList();
    private ObservableList<GeneralItemSelectedTm> selectedItemsList = FXCollections.observableArrayList();
    private PlaceOrderFormController parentController;
    
    @FXML
    public void initialize() {
        // Check if user is super admin before loading general items
        if (!AuthorizationUtil.canAccessGeneralItems()) {
            // User is not super admin - show error and close dialog
            new Alert(Alert.AlertType.ERROR, 
                "Access Denied: General Items are only accessible by Super Admin users.\n" +
                "Your Role: " + UserSessionData.userRole).showAndWait();
            // Close the dialog
            if (dialogContext != null && dialogContext.getScene() != null) {
                Stage stage = (Stage) dialogContext.getScene().getWindow();
                if (stage != null) {
                    stage.close();
                }
            }
            return;
        }
        
        loadProducts();
        setupTableColumns();
        setupTransportFeeListener();
        updateLabels();
    }
    
    public void setParentController(PlaceOrderFormController controller) {
        this.parentController = controller;
    }
    
    private void loadProducts() {
        // Double check super admin access before loading
        if (!UserSessionData.isSuperAdmin()) {
            productList.clear();
            tblProducts.setItems(productList);
            new Alert(Alert.AlertType.ERROR, 
                "Access Denied: General Items are only accessible by Super Admin users.").show();
            return;
        }
        
        try {
            List<GeneralItem> generalItems = generalItemService.findAllGeneralItems();
            productList.clear();
            
            for (GeneralItem item : generalItems) {
                CheckBox checkBox = new CheckBox();
                String itemName = item.getName() != null ? item.getName() : "Unnamed Item";
                
                checkBox.setOnAction(e -> {
                    if (checkBox.isSelected()) {
                        addToSelectedItems(itemName);
                    } else {
                        removeFromSelectedItems(itemName);
                    }
                });
                
                GeneralItemTm tm = new GeneralItemTm(itemName, checkBox);
                productList.add(tm);
            }
            
            tblProducts.setItems(productList);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading general items: " + e.getMessage()).show();
        }
    }
    
    private void setupTableColumns() {
        // Products table
        if (colProductName != null) {
            colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        }
        
        if (colSelect != null) {
            colSelect.setCellValueFactory(cellData -> {
                GeneralItemTm item = cellData.getValue();
                return new javafx.beans.property.SimpleObjectProperty<>(item.getSelect());
            });
            colSelect.setCellFactory(column -> new TableCell<GeneralItemTm, CheckBox>() {
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
        }
        
        // Selected items table
        if (colSelProductName != null) {
            colSelProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        }
        
        if (colSelQuantity != null) {
            colSelQuantity.setCellValueFactory(cellData -> {
                GeneralItemSelectedTm item = cellData.getValue();
                return new javafx.beans.property.SimpleObjectProperty<>(item.getQuantity());
            });
            colSelQuantity.setCellFactory(column -> new TableCell<GeneralItemSelectedTm, TextField>() {
                @Override
                protected void updateItem(TextField textField, boolean empty) {
                    super.updateItem(textField, empty);
                    if (empty || textField == null) {
                        setGraphic(null);
                    } else {
                        setGraphic(textField);
                        setAlignment(Pos.CENTER);
                        // Add listener to recalculate total when quantity changes
                        textField.textProperty().addListener((obs, oldVal, newVal) -> {
                            if (!empty && textField.getParent() != null) {
                                int index = getIndex();
                                if (index >= 0 && getTableView() != null && getTableView().getItems() != null 
                                    && index < getTableView().getItems().size()) {
                                    GeneralItemSelectedTm item = getTableView().getItems().get(index);
                                calculateItemTotal(item);
                                calculateTotals();
                                }
                            }
                        });
                    }
                }
            });
        }
        
        if (colSelUnitPrice != null) {
            colSelUnitPrice.setCellValueFactory(cellData -> {
                GeneralItemSelectedTm item = cellData.getValue();
                return new javafx.beans.property.SimpleObjectProperty<>(item.getUnitPrice());
            });
            colSelUnitPrice.setCellFactory(column -> new TableCell<GeneralItemSelectedTm, TextField>() {
                @Override
                protected void updateItem(TextField textField, boolean empty) {
                    super.updateItem(textField, empty);
                    if (empty || textField == null) {
                        setGraphic(null);
                    } else {
                        setGraphic(textField);
                        setAlignment(Pos.CENTER);
                        // Add listener to recalculate total when price changes
                        textField.textProperty().addListener((obs, oldVal, newVal) -> {
                            if (!empty && textField.getParent() != null) {
                                int index = getIndex();
                                if (index >= 0 && getTableView() != null && getTableView().getItems() != null 
                                    && index < getTableView().getItems().size()) {
                                    GeneralItemSelectedTm item = getTableView().getItems().get(index);
                                calculateItemTotal(item);
                                calculateTotals();
                                }
                            }
                        });
                    }
                }
            });
        }
        
        if (colSelTotal != null) {
            colSelTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
            colSelTotal.setCellFactory(column -> new TableCell<GeneralItemSelectedTm, Double>() {
                @Override
                protected void updateItem(Double total, boolean empty) {
                    super.updateItem(total, empty);
                    if (empty || total == null) {
                        setText(null);
                    } else {
                        setText(String.format("%.2f", total));
                    }
                }
            });
        }
        
        if (colSelRemove != null) {
            colSelRemove.setCellValueFactory(cellData -> {
                GeneralItemSelectedTm item = cellData.getValue();
                return new javafx.beans.property.SimpleObjectProperty<>(item.getBtnRemove());
            });
            colSelRemove.setCellFactory(column -> new TableCell<GeneralItemSelectedTm, Button>() {
                @Override
                protected void updateItem(Button button, boolean empty) {
                    super.updateItem(button, empty);
                    if (empty || button == null) {
                        setGraphic(null);
                    } else {
                        setGraphic(button);
                        setAlignment(Pos.CENTER);
                    }
                }
            });
        }
        
        tblSelectedItems.setItems(selectedItemsList);
    }
    
    private void addToSelectedItems(String productName) {
        // Check if already added
        for (GeneralItemSelectedTm item : selectedItemsList) {
            if (item.getProductName().equals(productName)) {
                return; // Already added
            }
        }
        
        TextField quantityField = new TextField("1");
        quantityField.setPrefWidth(100);
        // Allow any text input for quantity (no validation restriction)
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Allow any text input - user can enter strings like "2 kg", "3 pieces", etc.
            // Recalculate totals when quantity changes
            if (newVal != null) {
                calculateItemTotal(selectedItemsList.stream()
                    .filter(item -> item.getQuantity() == quantityField)
                    .findFirst()
                    .orElse(null));
                calculateTotals();
            }
        });
        
        TextField priceField = new TextField("0.00");
        priceField.setPrefWidth(100);
        priceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("^\\d*\\.?\\d*$")) {
                priceField.setText(oldVal);
            }
        });
        
        Button removeBtn = new Button("Remove");
        removeBtn.setOnAction(e -> {
            selectedItemsList.removeIf(item -> item.getProductName().equals(productName));
            // Uncheck the checkbox in products table
            for (GeneralItemTm item : productList) {
                if (item.getProductName().equals(productName)) {
                    item.getSelect().setSelected(false);
                    break;
                }
            }
            calculateTotals();
        });
        
        GeneralItemSelectedTm tm = new GeneralItemSelectedTm(productName, quantityField, priceField, 0.0, removeBtn);
        selectedItemsList.add(tm);
        calculateItemTotal(tm);
        calculateTotals();
    }
    
    private void removeFromSelectedItems(String productName) {
        selectedItemsList.removeIf(item -> item.getProductName().equals(productName));
        calculateTotals();
    }
    
    private void calculateItemTotal(GeneralItemSelectedTm item) {
        if (item == null) return;
        
        try {
            double quantity = 0.0;
            String qtyText = item.getQuantity().getText();
            if (qtyText != null && !qtyText.trim().isEmpty()) {
                // Try to parse as number, if it fails, use 0 (allows text input)
                try {
                    // Extract numeric value from text (e.g., "2 kg" -> 2.0)
                    String numericPart = qtyText.trim().replaceAll("[^0-9.]", "").split("\\s")[0];
                    if (!numericPart.isEmpty()) {
                        quantity = Double.parseDouble(numericPart);
                    }
                } catch (NumberFormatException e) {
                    // If text doesn't contain valid number, quantity remains 0
                    quantity = 0.0;
                }
            }
            
            double unitPrice = 0.0;
            String priceText = item.getUnitPrice().getText();
            if (priceText != null && !priceText.trim().isEmpty()) {
                unitPrice = Double.parseDouble(priceText.trim());
            }
            
            double total = quantity * unitPrice;
            item.setTotal(total);
            tblSelectedItems.refresh();
        } catch (NumberFormatException e) {
            item.setTotal(0.0);
            tblSelectedItems.refresh();
        }
    }
    
    private void setupTransportFeeListener() {
        if (txtTransportFee != null) {
            txtTransportFee.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.matches("^\\d*\\.?\\d*$")) {
                    txtTransportFee.setText(oldValue);
                } else {
                    calculateTotals();
                }
            });
        }
    }
    
    private void calculateTotals() {
        double subtotal = 0.0;
        for (GeneralItemSelectedTm item : selectedItemsList) {
            calculateItemTotal(item);
            subtotal += item.getTotal();
        }
        
        double transportFee = 0.0;
        try {
            if (txtTransportFee != null && txtTransportFee.getText() != null && !txtTransportFee.getText().trim().isEmpty()) {
                transportFee = Double.parseDouble(txtTransportFee.getText().trim());
            }
        } catch (NumberFormatException e) {
            transportFee = 0.0;
        }
        
        double total = subtotal + transportFee;
        updateLabels(subtotal, transportFee, total);
    }
    
    private void updateLabels() {
        updateLabels(0.0, 0.0, 0.0);
    }
    
    private void updateLabels(double subtotal, double transportFee, double total) {
        if (lblSubtotal != null) {
            lblSubtotal.setText(String.format("%.2f /=", subtotal));
        }
        if (lblTransportFee != null) {
            lblTransportFee.setText(String.format("%.2f /=", transportFee));
        }
        if (lblTotal != null) {
            lblTotal.setText(String.format("%.2f /=", total));
        }
    }
    
    @FXML
    public void btnCancel(ActionEvent actionEvent) {
        Stage stage = (Stage) dialogContext.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    public void btnAddToCart(ActionEvent actionEvent) {
        if (selectedItemsList.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please select at least one item!").show();
            return;
        }
        
        // Validate all items have quantity and price
        List<String> errors = new ArrayList<>();
        for (GeneralItemSelectedTm item : selectedItemsList) {
            String qtyText = item.getQuantity().getText();
            String priceText = item.getUnitPrice().getText();
            
            // Quantity can be any text (e.g., "2 kg", "3 pieces", etc.)
            // Just check that it's not empty
            if (qtyText == null || qtyText.trim().isEmpty()) {
                errors.add(item.getProductName() + ": Quantity is required");
            }
            
            // Price must be a valid number
            if (priceText == null || priceText.trim().isEmpty()) {
                errors.add(item.getProductName() + ": Price is required");
            } else {
                try {
                    double price = Double.parseDouble(priceText.trim());
                    if (price < 0) {
                        errors.add(item.getProductName() + ": Price cannot be negative");
                    }
                } catch (NumberFormatException e) {
                    errors.add(item.getProductName() + ": Invalid price");
                }
            }
        }
        
        if (!errors.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please fix the following errors:\n\n" + String.join("\n", errors)).show();
            return;
        }
        
        // Get transport fee
        double transportFee = 0.0;
        try {
            if (txtTransportFee != null && txtTransportFee.getText() != null && !txtTransportFee.getText().trim().isEmpty()) {
                transportFee = Double.parseDouble(txtTransportFee.getText().trim());
            }
        } catch (NumberFormatException e) {
            // Transport fee is optional, so ignore error
        }
        
        // Pass items to parent controller
        if (parentController != null) {
            parentController.addGeneralItemsToCart(selectedItemsList, transportFee);
        }
        
        // Close dialog
        Stage stage = (Stage) dialogContext.getScene().getWindow();
        stage.close();
    }
}
