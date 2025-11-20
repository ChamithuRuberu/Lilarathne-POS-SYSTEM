package com.devstack.pos.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.devstack.pos.entity.Supplier;
import com.devstack.pos.service.SupplierService;
import com.devstack.pos.util.AuthorizationUtil;
import com.devstack.pos.view.tm.SupplierTm;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SupplierManagementController extends BaseController {
    
    private final SupplierService supplierService;
    
    @FXML
    private JFXTextField txtName;
    
    @FXML
    private JFXTextField txtEmail;
    
    @FXML
    private JFXTextField txtPhone;
    
    @FXML
    private JFXTextField txtContactPerson;
    
    @FXML
    private JFXTextField txtAddress;
    
    @FXML
    private JFXTextField txtSearch;
    
    @FXML
    private ComboBox<String> cmbStatus;
    
    @FXML
    private JFXButton btnSave;
    
    @FXML
    private JFXButton btnUpdate;
    
    @FXML
    private Text lblTotalSuppliers;
    
    @FXML
    private Text lblActiveSuppliers;
    
    @FXML
    private Text lblInactiveSuppliers;
    
    @FXML
    private TableView<SupplierTm> tblSuppliers;
    
    @FXML
    private TableColumn<SupplierTm, Long> colId;
    
    @FXML
    private TableColumn<SupplierTm, String> colName;
    
    @FXML
    private TableColumn<SupplierTm, String> colEmail;
    
    @FXML
    private TableColumn<SupplierTm, String> colPhone;
    
    @FXML
    private TableColumn<SupplierTm, String> colContactPerson;
    
    @FXML
    private TableColumn<SupplierTm, String> colAddress;
    
    @FXML
    private TableColumn<SupplierTm, String> colStatus;
    
    @FXML
    private TableColumn<SupplierTm, HBox> colAction;
    
    private Long selectedSupplierId = null;
    private String searchText = "";
    
    @FXML
    public void initialize() {
        // Fix table to prevent extra column
        tblSuppliers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // Check authorization
        if (!AuthorizationUtil.isAdmin()) {
            showError("Access Denied", "You don't have permission to access this page.");
            return;
        }
        
        // Configure supplier table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colContactPerson.setCellValueFactory(new PropertyValueFactory<>("contactPerson"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Configure action column with custom cell factory for HBox
        colAction.setCellValueFactory(param -> {
            SupplierTm supplierTm = param.getValue();
            return new SimpleObjectProperty<>(
                supplierTm != null ? supplierTm.getActionButtons() : null);
        });
        
        colAction.setCellFactory(column -> new TableCell<SupplierTm, HBox>() {
            @Override
            protected void updateItem(HBox item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(item);
                }
            }
        });
        
        // Status combo box - Initialize BEFORE loading data
        ObservableList<String> statusList = FXCollections.observableArrayList("All", "ACTIVE", "INACTIVE");
        cmbStatus.setItems(statusList);
        cmbStatus.setValue("All");
        
        // Load data
        loadSuppliers();
        loadStatistics();
        
        // Search listener
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            searchText = newValue;
            loadSuppliers();
        });
        
        // Status filter listener
        cmbStatus.setOnAction(event -> loadSuppliers());
    }
    
    @Override
    protected String getCurrentPageName() {
        return "Supplier Management";
    }
    
    @FXML
    public void btnBackToDashboard(ActionEvent actionEvent) {
        navigateTo("DashboardForm", true);
    }
    
    @FXML
    public void btnRefresh(ActionEvent event) {
        loadSuppliers();
        loadStatistics();
        clearFields();
    }
    
    @FXML
    public void btnSave(ActionEvent event) {
        try {
            if (txtName.getText().trim().isEmpty()) {
                showWarning("Validation Error", "Supplier name is required!");
                return;
            }
            
            Supplier supplier = new Supplier();
            supplier.setName(txtName.getText().trim());
            supplier.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
            supplier.setPhone(txtPhone.getText().trim().isEmpty() ? null : txtPhone.getText().trim());
            supplier.setContactPerson(txtContactPerson.getText().trim().isEmpty() ? null : txtContactPerson.getText().trim());
            supplier.setAddress(txtAddress.getText().trim().isEmpty() ? null : txtAddress.getText().trim());
            supplier.setNotes(null); // Removed from form
            supplier.setStatus("ACTIVE");
            supplier.setCreatedAt(LocalDateTime.now());
            supplier.setUpdatedAt(LocalDateTime.now());
            
            supplierService.saveSupplier(supplier);
            showSuccess("Success", "Supplier saved successfully!");
            clearFields();
            loadSuppliers();
            loadStatistics();
        } catch (IllegalArgumentException e) {
            showWarning("Validation Error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Failed to save supplier: " + e.getMessage());
        }
    }
    
    @FXML
    public void btnUpdate(ActionEvent event) {
        try {
            if (selectedSupplierId == null) {
                showWarning("Selection Error", "Please select a supplier to update!");
                return;
            }
            
            if (txtName.getText().trim().isEmpty()) {
                showWarning("Validation Error", "Supplier name is required!");
                return;
            }
            
            Optional<Supplier> existingSupplier = Optional.ofNullable(supplierService.findSupplier(selectedSupplierId));
            if (existingSupplier.isEmpty()) {
                showError("Error", "Supplier not found!");
                return;
            }
            
            Supplier supplier = existingSupplier.get();
            supplier.setName(txtName.getText().trim());
            supplier.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
            supplier.setPhone(txtPhone.getText().trim().isEmpty() ? null : txtPhone.getText().trim());
            supplier.setContactPerson(txtContactPerson.getText().trim().isEmpty() ? null : txtContactPerson.getText().trim());
            supplier.setAddress(txtAddress.getText().trim().isEmpty() ? null : txtAddress.getText().trim());
            supplier.setUpdatedAt(LocalDateTime.now());
            
            supplierService.updateSupplier(supplier);
            showSuccess("Success", "Supplier updated successfully!");
            clearFields();
            loadSuppliers();
            loadStatistics();
        } catch (IllegalArgumentException e) {
            showWarning("Validation Error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Failed to update supplier: " + e.getMessage());
        }
    }
    
    @FXML
    public void btnClear(ActionEvent event) {
        clearFields();
    }
    
    private void clearFields() {
        txtName.clear();
        txtEmail.clear();
        txtPhone.clear();
        txtContactPerson.clear();
        txtAddress.clear();
        selectedSupplierId = null;
        btnSave.setVisible(true);
        btnUpdate.setVisible(false);
    }
    
    public void setData(SupplierTm tm) {
        selectedSupplierId = tm.getId();
        txtName.setText(tm.getName());
        txtEmail.setText(tm.getEmail() != null ? tm.getEmail() : "");
        txtPhone.setText(tm.getPhone() != null ? tm.getPhone() : "");
        txtContactPerson.setText(tm.getContactPerson() != null ? tm.getContactPerson() : "");
        txtAddress.setText(tm.getAddress() != null ? tm.getAddress() : "");
        
        btnSave.setVisible(false);
        btnUpdate.setVisible(true);
    }
    
    private void loadSuppliers() {
        try {
            ObservableList<SupplierTm> data = FXCollections.observableArrayList();
            
            String status = cmbStatus.getValue();
            // Default to "All" if status is null
            if (status == null) {
                status = "All";
            }
            
            List<Supplier> suppliers;
            
            if (searchText != null && !searchText.trim().isEmpty()) {
                suppliers = supplierService.searchSuppliers(searchText.trim());
                if (!"All".equals(status)) {
                    String finalStatus = status;
                    suppliers = suppliers.stream()
                            .filter(s -> s.getStatus() != null && finalStatus.equals(s.getStatus()))
                            .toList();
                }
            } else {
                suppliers = supplierService.findAllSuppliers();
                if (!"All".equals(status)) {
                    String finalStatus1 = status;
                    suppliers = suppliers.stream()
                            .filter(s -> s.getStatus() != null && finalStatus1.equals(s.getStatus()))
                            .toList();
                }
            }
            
            for (Supplier supplier : suppliers) {
                HBox actionButtons = createActionButtons(supplier);
                // Handle null status - default to "INACTIVE" for display
                String displayStatus = supplier.getStatus() != null ? supplier.getStatus() : "INACTIVE";
                
                SupplierTm tm = new SupplierTm(
                    supplier.getId(),
                    supplier.getName(),
                    supplier.getEmail() != null ? supplier.getEmail() : "",
                    supplier.getPhone() != null ? supplier.getPhone() : "",
                    supplier.getAddress() != null ? supplier.getAddress() : "",
                    supplier.getContactPerson() != null ? supplier.getContactPerson() : "",
                    displayStatus,
                    actionButtons
                );
                data.add(tm);
            }
            
            tblSuppliers.setItems(data);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Failed to load suppliers: " + e.getMessage());
        }
    }
    
    private HBox createActionButtons(Supplier supplier) {
        HBox hbox = new HBox(5);
        hbox.setStyle("-fx-alignment: center;");
        
        JFXButton editBtn = new JFXButton("Edit");
        editBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 5;");
        editBtn.setOnAction(e -> {
            SupplierTm tm = new SupplierTm(
                supplier.getId(),
                supplier.getName(),
                supplier.getEmail() != null ? supplier.getEmail() : "",
                supplier.getPhone() != null ? supplier.getPhone() : "",
                supplier.getAddress() != null ? supplier.getAddress() : "",
                supplier.getContactPerson() != null ? supplier.getContactPerson() : "",
                supplier.getStatus() != null ? supplier.getStatus() : "INACTIVE",
                null
            );
            setData(tm);
        });
        
        JFXButton deleteBtn = new JFXButton("Delete");
        deleteBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 5;");
        deleteBtn.setOnAction(e -> deleteSupplier(supplier.getId()));
        
        hbox.getChildren().addAll(editBtn, deleteBtn);
        return hbox;
    }
    
    private void deleteSupplier(Long id) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Supplier");
        confirmDialog.setContentText("Are you sure you want to delete this supplier?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                supplierService.deleteSupplier(id);
                showSuccess("Success", "Supplier deleted successfully!");
                clearFields();
                loadSuppliers();
                loadStatistics();
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error", "Failed to delete supplier: " + e.getMessage());
            }
        }
    }
    
    private void loadStatistics() {
        try {
            long total = supplierService.countSuppliers();
            long active = supplierService.countActiveSuppliers();
            long inactive = total - active;
            
            lblTotalSuppliers.setText(String.valueOf(total));
            lblActiveSuppliers.setText(String.valueOf(active));
            lblInactiveSuppliers.setText(String.valueOf(inactive));
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Failed to load statistics: " + e.getMessage());
        }
    }
}
