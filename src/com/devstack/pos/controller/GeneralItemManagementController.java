package com.devstack.pos.controller;

import com.devstack.pos.entity.GeneralItem;
import com.devstack.pos.service.GeneralItemService;
import com.devstack.pos.util.AuthorizationUtil;
import com.devstack.pos.util.UserSessionData;
import com.devstack.pos.view.tm.GeneralItemManagementTm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeneralItemManagementController {
    
    public TextField txtItemName;
    public Button btnSave;
    public Button btnClose;
    public TableView<GeneralItemManagementTm> tblGeneralItems;
    public TableColumn colItemId;
    public TableColumn colItemName;
    public TableColumn colItemDelete;
    
    private final GeneralItemService generalItemService;
    private Long selectedItemId = null;
    
    public void initialize() {
        // Check authorization
        if (!AuthorizationUtil.canAccessGeneralItems()) {
            new Alert(Alert.AlertType.ERROR, 
                "Access Denied: General Items can only be managed by Super Admin users.\n" +
                "Your Role: " + UserSessionData.userRole).showAndWait();
            // Close the window
            if (btnClose != null && btnClose.getScene() != null) {
                Stage stage = (Stage) btnClose.getScene().getWindow();
                if (stage != null) {
                    stage.close();
                }
            }
            return;
        }
        
        // Fix table to prevent extra column
        tblGeneralItems.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Setup table columns
        colItemId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colItemDelete.setCellValueFactory(new PropertyValueFactory<>("delete"));
        
        // Load general items
        loadGeneralItems();
        
        // Add selection listener
        tblGeneralItems.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadGeneralItemData(newVal);
            }
        });
    }
    
    private void loadGeneralItems() {
        try {
            ObservableList<GeneralItemManagementTm> items = FXCollections.observableArrayList();
            
            for (GeneralItem item : generalItemService.findAllGeneralItems()) {
                Button deleteBtn = new Button("Delete");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 15; -fx-font-size: 12px;");
                
                GeneralItemManagementTm tm = new GeneralItemManagementTm(
                    item.getId(),
                    item.getName(),
                    deleteBtn
                );
                
                items.add(tm);
                
                deleteBtn.setOnAction(e -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Are you sure you want to delete general item: " + item.getName() + "?",
                        ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            if (generalItemService.deleteGeneralItem(item.getId())) {
                                new Alert(Alert.AlertType.INFORMATION, "General item deleted successfully!").show();
                                loadGeneralItems();
                                clearFields();
                            } else {
                                new Alert(Alert.AlertType.ERROR, "Failed to delete general item!").show();
                            }
                        }
                    });
                });
            }
            
            tblGeneralItems.setItems(items);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading general items: " + e.getMessage()).show();
        }
    }
    
    private void loadGeneralItemData(GeneralItemManagementTm tm) {
        try {
            GeneralItem item = generalItemService.findGeneralItem(tm.getId());
            if (item != null) {
                selectedItemId = item.getId();
                txtItemName.setText(item.getName());
                btnSave.setText("Update Item");
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading general item: " + e.getMessage()).show();
        }
    }
    
    public void btnSaveOnAction(ActionEvent actionEvent) {
        try {
            // Check authorization again
            if (!AuthorizationUtil.canAccessGeneralItems()) {
                AuthorizationUtil.showUnauthorizedAlert();
                return;
            }
            
            String name = txtItemName.getText().trim();
            
            if (name.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please enter an item name!").show();
                return;
            }
            
            GeneralItem generalItem = new GeneralItem();
            generalItem.setName(name);
            
            if (selectedItemId == null) {
                // New item
                generalItemService.saveGeneralItem(generalItem);
                new Alert(Alert.AlertType.INFORMATION, "General item saved successfully!").show();
            } else {
                // Update existing
                generalItem.setId(selectedItemId);
                if (generalItemService.updateGeneralItem(generalItem)) {
                    new Alert(Alert.AlertType.INFORMATION, "General item updated successfully!").show();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Failed to update general item!").show();
                    return;
                }
            }
            
            clearFields();
            loadGeneralItems();
            
        } catch (SecurityException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error saving general item: " + e.getMessage()).show();
        }
    }
    
    public void btnClearOnAction(ActionEvent actionEvent) {
        clearFields();
    }
    
    public void btnCloseOnAction(ActionEvent actionEvent) {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
    
    private void clearFields() {
        txtItemName.clear();
        selectedItemId = null;
        btnSave.setText("Save Item");
        tblGeneralItems.getSelectionModel().clearSelection();
    }
}

