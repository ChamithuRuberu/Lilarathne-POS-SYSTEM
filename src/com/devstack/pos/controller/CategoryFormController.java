package com.devstack.pos.controller;

import com.devstack.pos.entity.Category;
import com.devstack.pos.enums.Status;
import com.devstack.pos.service.CategoryService;
import com.devstack.pos.view.tm.CategoryTm;
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
public class CategoryFormController {
    
    public TextField txtCategoryName;
    public TextArea txtDescription;
    public Button btnSave;
    public Button btnClose;
    public TableView<CategoryTm> tblCategories;
    public TableColumn colCategoryId;
    public TableColumn colCategoryName;
    public TableColumn colCategoryDescription;
    public TableColumn colCategoryStatus;
    public TableColumn colCategoryDelete;
    
    private final CategoryService categoryService;
    private Integer selectedCategoryId = null;
    
    public void initialize() {
        // Fix table to prevent extra column
        tblCategories.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Setup table columns
        colCategoryId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCategoryName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategoryDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCategoryStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCategoryDelete.setCellValueFactory(new PropertyValueFactory<>("delete"));
        
        // Load categories
        loadCategories();
        
        // Add selection listener
        tblCategories.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadCategoryData(newVal);
            }
        });
    }
    
    private void loadCategories() {
        try {
            ObservableList<CategoryTm> categories = FXCollections.observableArrayList();
            
            for (Category category : categoryService.findAllCategories()) {
                Button deleteBtn = new Button("Delete");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 15; -fx-font-size: 12px;");
                
                CategoryTm tm = new CategoryTm(
                    category.getId(),
                    category.getName(),
                    category.getDescription(),
                    category.getStatus().toString(),
                    deleteBtn
                );
                
                categories.add(tm);
                
                deleteBtn.setOnAction(e -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Are you sure you want to delete category: " + category.getName() + "?",
                        ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            if (categoryService.deleteCategory(category.getId())) {
                                new Alert(Alert.AlertType.INFORMATION, "Category deleted successfully!").show();
                                loadCategories();
                                clearFields();
                            } else {
                                new Alert(Alert.AlertType.ERROR, "Failed to delete category!").show();
                            }
                        }
                    });
                });
            }
            
            tblCategories.setItems(categories);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading categories: " + e.getMessage()).show();
        }
    }
    
    private void loadCategoryData(CategoryTm tm) {
        try {
            Category category = categoryService.findCategory(tm.getId());
            if (category != null) {
                selectedCategoryId = category.getId();
                txtCategoryName.setText(category.getName());
                txtDescription.setText(category.getDescription());
                btnSave.setText("Update Category");
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading category: " + e.getMessage()).show();
        }
    }
    
    public void btnSaveOnAction(ActionEvent actionEvent) {
        try {
            String name = txtCategoryName.getText().trim();
            String description = txtDescription.getText().trim();
            
            if (name.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please enter a category name!").show();
                return;
            }
            
            Category category = new Category();
            category.setName(name);
            category.setDescription(description.isEmpty() ? null : description);
            category.setStatus(Status.ACTIVE);
            
            if (selectedCategoryId == null) {
                // New category
                categoryService.saveCategory(category);
                new Alert(Alert.AlertType.INFORMATION, "Category saved successfully!").show();
            } else {
                // Update existing
                category.setId(selectedCategoryId);
                if (categoryService.updateCategory(category)) {
                    new Alert(Alert.AlertType.INFORMATION, "Category updated successfully!").show();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Failed to update category!").show();
                    return;
                }
            }
            
            clearFields();
            loadCategories();
            
        } catch (IllegalArgumentException e) {
            new Alert(Alert.AlertType.WARNING, e.getMessage()).show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error saving category: " + e.getMessage()).show();
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
        txtCategoryName.clear();
        txtDescription.clear();
        selectedCategoryId = null;
        btnSave.setText("Save Category");
        tblCategories.getSelectionModel().clearSelection();
    }
}

