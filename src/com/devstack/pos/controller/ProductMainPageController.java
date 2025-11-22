package com.devstack.pos.controller;

import com.devstack.pos.entity.Category;
import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.service.CategoryService;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.ProductService;
import com.devstack.pos.service.SupplierService;
import com.devstack.pos.view.tm.ProductDetailTm;
import com.devstack.pos.view.tm.ProductTm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductMainPageController extends BaseController {
    private final ProductService productService;
    private final ProductDetailService productDetailService;
    private final CategoryService categoryService;
    private final SupplierService supplierService;
    public TextArea txtProductDescription;
    public Button btnSaveUpdate;
    public ComboBox<String> cmbCategory;
    public ImageView imgBarcode;
    public VBox barcodeImageContainer;
    public Button btnGenerateBarcode;
    public TableView<ProductTm> tbl;
    public TableColumn colProductBarcode;
    public TableColumn colProductDesc;
    public TableColumn colProductCategory;
    public TableColumn colProductViewBarcode;
    // public TableColumn colProductShowMore; // Removed - Batches column no longer needed
    public TableColumn colProductDelete;
    public TextField txtSelectedProdId;
    public TextArea txtSelectedProdDescription;
    public Button btnNewBatch;
    public ScrollPane scrollPaneBatchDetails;
    public TableView<ProductDetailTm> tblDetail;
    public TableColumn colPDId;
    public TableColumn colPDQty;
    public TableColumn colPDSellingPrice;
    public TableColumn colPDBuyingPrice;
    public TableColumn colPDDAvailability;
    // public TableColumn colPDShowPrice; // Commented out - Show Price logic removed
    public TableColumn colPDSupplierName;
    public TableColumn colPDViewBarcode;
    public TableColumn colPDDelete;
    public com.jfoenix.controls.JFXTextField txtSearchProducts;
    private final String searchText = "";
    private Integer currentProductCode = null;
    private FilteredList<ProductTm> filteredProducts;
    private ObservableList<ProductTm> allProducts;
    private boolean showLowStockOnly = false; // Flag to show only low stock items

    /**
     * Set the low stock only filter mode
     * @param showLowStockOnly true to show only products with low stock batches
     */
    public void setShowLowStockOnly(boolean showLowStockOnly) {
        this.showLowStockOnly = showLowStockOnly;
        // Reload products with the new filter (always reload to apply/remove filter)
        loadAllProducts(searchText);
    }

    public void initialize() {
        // Reset low stock filter flag to false by default
        // This ensures normal navigation shows all products
        // The flag will be set to true only when explicitly called from dashboard low stock card
        showLowStockOnly = false;
        
        // Initialize sidebar
        initializeSidebar();

        // Fix tables to prevent extra column
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (tblDetail != null) {
            tblDetail.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }

        // Batch details scroll pane settings - hide scrollbars
        if (scrollPaneBatchDetails != null) {
            scrollPaneBatchDetails.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPaneBatchDetails.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPaneBatchDetails.setStyle("-fx-background-color: transparent;");
        }

        // Setup table columns
        colProductBarcode.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        colProductDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colProductCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colProductViewBarcode.setCellValueFactory(new PropertyValueFactory<>("viewBarcode"));
        colProductDelete.setCellValueFactory(new PropertyValueFactory<>("delete"));

        // Setup batch detail table columns
        if (tblDetail != null) {
        colPDId.setCellValueFactory(new PropertyValueFactory<>("code"));
        // Quantity column - supports decimal display (e.g., 2.5, 3.75)
        colPDQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colPDQty.setCellFactory(column -> new TableCell<ProductDetailTm, Double>() {
            @Override
            protected void updateItem(Double qty, boolean empty) {
                super.updateItem(qty, empty);
                if (empty || qty == null) {
                    setText(null);
                } else {
                    // Show as integer if whole number, otherwise show decimals
                    if (qty == qty.intValue()) {
                        setText(String.valueOf(qty.intValue()));
                    } else {
                        setText(String.format("%.2f", qty));
                    }
                }
            }
        });
        colPDSellingPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        colPDBuyingPrice.setCellValueFactory(new PropertyValueFactory<>("buyingPrice"));
        colPDDAvailability.setCellValueFactory(new PropertyValueFactory<>("discountAvailability"));
            // colPDShowPrice.setCellValueFactory(new PropertyValueFactory<>("showPrice")); // Commented out
            if (colPDSupplierName != null) {
                colPDSupplierName.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
            }
            colPDViewBarcode.setCellValueFactory(new PropertyValueFactory<>("viewBarcode"));
        colPDDelete.setCellValueFactory(new PropertyValueFactory<>("delete"));
        }

        // Load categories
        loadCategories();

        // Load products
        loadAllProducts(searchText);

        // Setup search filter listener
        if (txtSearchProducts != null) {
            txtSearchProducts.textProperty().addListener((observable, oldValue, newValue) -> {
                filterProducts(newValue);
            });
        }

        tbl.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        setData(newValue);
                    }
                });

        // Batch table selection listener
        if (tblDetail != null) {
        tblDetail.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    try {
                        if (newValue != null) {
                            loadExternalUi(true, newValue);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
        }

        // Hide scrollbars in tables programmatically
        javafx.application.Platform.runLater(() -> {
            hideTableScrollBars(tbl);
            if (tblDetail != null) {
                hideTableScrollBars(tblDetail);
            }
            // Also hide scrollbars in ScrollPanes
            if (scrollPaneBatchDetails != null) {
                hideScrollPaneScrollBars(scrollPaneBatchDetails);
            }
        });
    }

    /**
     * Hides scrollbars in a ScrollPane
     */
    private void hideScrollPaneScrollBars(ScrollPane scrollPane) {
        if (scrollPane == null) return;
        
        // Set policies to NEVER
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        // Find and hide scrollbars using lookup
        scrollPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                javafx.application.Platform.runLater(() -> {
                    hideScrollBarsInScrollPane(scrollPane);
                    newScene.addPostLayoutPulseListener(() -> {
                        hideScrollBarsInScrollPane(scrollPane);
                    });
                });
            }
        });
        
        if (scrollPane.getScene() != null) {
            javafx.application.Platform.runLater(() -> {
                hideScrollBarsInScrollPane(scrollPane);
            });
        }
    }

    /**
     * Hides scrollbars in a ScrollPane using lookup
     */
    private void hideScrollBarsInScrollPane(ScrollPane scrollPane) {
        if (scrollPane == null || scrollPane.getScene() == null) return;
        
        javafx.scene.control.ScrollBar vScrollBar = (javafx.scene.control.ScrollBar) 
            scrollPane.lookup(".scroll-bar:vertical");
        javafx.scene.control.ScrollBar hScrollBar = (javafx.scene.control.ScrollBar) 
            scrollPane.lookup(".scroll-bar:horizontal");
        
        if (vScrollBar != null) {
            vScrollBar.setVisible(false);
            vScrollBar.setManaged(false);
            vScrollBar.setPrefWidth(0);
            vScrollBar.setMinWidth(0);
            vScrollBar.setMaxWidth(0);
            vScrollBar.setStyle("-fx-opacity: 0; -fx-pref-width: 0; -fx-min-width: 0; -fx-max-width: 0;");
        }
        
        if (hScrollBar != null) {
            hScrollBar.setVisible(false);
            hScrollBar.setManaged(false);
            hScrollBar.setPrefHeight(0);
            hScrollBar.setMinHeight(0);
            hScrollBar.setMaxHeight(0);
            hScrollBar.setStyle("-fx-opacity: 0; -fx-pref-height: 0; -fx-min-height: 0; -fx-max-height: 0;");
        }
        
        // Recursively search for any scrollbars
        hideScrollBarsRecursive(scrollPane);
    }

    /**
     * Recursively hides all scrollbars in a TableView
     */
    private void hideTableScrollBars(TableView<?> tableView) {
        if (tableView == null) return;
        
        // Find and hide scrollbars using lookup (more reliable)
        tableView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // Use multiple attempts to catch scrollbars as they're created
                javafx.application.Platform.runLater(() -> {
                    hideScrollBarsInTable(tableView);
                    // Also set up a listener for layout pulses to catch dynamically created scrollbars
                    newScene.addPostLayoutPulseListener(() -> {
                        hideScrollBarsInTable(tableView);
                    });
                });
            }
        });
        
        // Also hide immediately if scene is already set
        if (tableView.getScene() != null) {
            javafx.application.Platform.runLater(() -> {
                hideScrollBarsInTable(tableView);
            });
        }
    }

    /**
     * Hides scrollbars in a TableView using lookup
     */
    private void hideScrollBarsInTable(TableView<?> tableView) {
        if (tableView == null || tableView.getScene() == null) return;
        
        // Lookup scrollbars using JavaFX's lookup mechanism
        javafx.scene.control.ScrollBar vScrollBar = (javafx.scene.control.ScrollBar) 
            tableView.lookup(".scroll-bar:vertical");
        javafx.scene.control.ScrollBar hScrollBar = (javafx.scene.control.ScrollBar) 
            tableView.lookup(".scroll-bar:horizontal");
        
        if (vScrollBar != null) {
            vScrollBar.setVisible(false);
            vScrollBar.setManaged(false);
            vScrollBar.setPrefWidth(0);
            vScrollBar.setMinWidth(0);
            vScrollBar.setMaxWidth(0);
            vScrollBar.setStyle("-fx-opacity: 0; -fx-pref-width: 0; -fx-min-width: 0; -fx-max-width: 0;");
        }
        
        if (hScrollBar != null) {
            hScrollBar.setVisible(false);
            hScrollBar.setManaged(false);
            hScrollBar.setPrefHeight(0);
            hScrollBar.setMinHeight(0);
            hScrollBar.setMaxHeight(0);
            hScrollBar.setStyle("-fx-opacity: 0; -fx-pref-height: 0; -fx-min-height: 0; -fx-max-height: 0;");
        }
        
        // Also recursively search for any scrollbars
        hideScrollBarsRecursive(tableView);
    }

    /**
     * Recursively finds and hides all ScrollBar nodes
     */
    private void hideScrollBarsRecursive(javafx.scene.Node node) {
        if (node instanceof javafx.scene.control.ScrollBar) {
            javafx.scene.control.ScrollBar scrollBar = (javafx.scene.control.ScrollBar) node;
            scrollBar.setVisible(false);
            scrollBar.setManaged(false);
            scrollBar.setPrefWidth(0);
            scrollBar.setPrefHeight(0);
            scrollBar.setMinWidth(0);
            scrollBar.setMinHeight(0);
            scrollBar.setMaxWidth(0);
            scrollBar.setMaxHeight(0);
            scrollBar.setStyle("-fx-opacity: 0; -fx-pref-width: 0; -fx-pref-height: 0;");
        }
        
        if (node instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent) node;
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                hideScrollBarsRecursive(child);
            }
        }
    }

    private void loadCategories() {
        try {
            List<Category> categoryList = categoryService.findActiveCategories();
            System.out.println("Loading categories, found: " + categoryList.size());

            ObservableList<String> categories = FXCollections.observableArrayList();
            for (Category category : categoryList) {
                categories.add(category.getName());
            }
            cmbCategory.setItems(categories);

            if (categories.isEmpty()) {
                System.out.println("No categories found. Please add categories using the 'Manage' button.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading categories: " + e.getMessage());
            new Alert(Alert.AlertType.ERROR, "Error loading categories: " + e.getMessage()).show();
        }
    }
    
    @Override
    protected String getCurrentPageName() {
        return "Products";
    }

    private void setData(ProductTm newValue) {
        txtSelectedProdId.setText(String.valueOf(newValue.getCode()));
        txtSelectedProdDescription.setText(newValue.getDescription());
        btnNewBatch.setDisable(false);
        currentProductCode = newValue.getCode();
        
        // Load product data into form for editing
        try {
            Product product = productService.findProduct(newValue.getCode());
            if (product != null) {
                loadExistingProduct(product);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            loadBatchData(newValue.getCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void btnBackToHomeOnAction(ActionEvent actionEvent) {
        btnDashboardOnAction(actionEvent);
    }

    public void btnNewProductOnAction(ActionEvent actionEvent) {
        try {
            // Validate inputs
            if (txtProductDescription.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please enter a product description!").show();
                return;
            }

            if (cmbCategory.getValue() == null || cmbCategory.getValue().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please select a category!").show();
                return;
            }

            Product product = new Product();
            product.setDescription(txtProductDescription.getText().trim());

            // Set category
            Category category = categoryService.findCategoryByName(cmbCategory.getValue());
            if (category != null) {
                product.setCategory(category);
            }

            if (btnSaveUpdate.getText().equals("Save Product")) {
                // Barcode generation feature moved to Feature/scanner branch
                // Generate a simple unique identifier
                String barcodeValue = String.format("%012d", System.currentTimeMillis() % 1000000000000L);
                product.setBarcode(barcodeValue);
                System.out.println("[PRODUCT SAVE] Generated identifier: " + barcodeValue);
                
                Product savedProduct = productService.saveProduct(product);
                
                new Alert(Alert.AlertType.CONFIRMATION,
                        "Product Saved Successfully!\nIdentifier: " + savedProduct.getBarcode()).show();

                clearFields();
                loadAllProducts(searchText);
            } else {
                // Update existing product - keep existing barcode
                if (currentProductCode != null) {
                    Product existingProduct = productService.findProduct(currentProductCode);
                    if (existingProduct != null) {
                        // Preserve existing barcode when updating
                        product.setBarcode(existingProduct.getBarcode());
                    }
                    product.setCode(currentProductCode);
                    if (productService.updateProduct(product)) {
                        new Alert(Alert.AlertType.CONFIRMATION, "Product Updated!").show();
                        clearFields();
                        loadAllProducts(searchText);
                        btnSaveUpdate.setText("Save Product");
                    } else {
                        new Alert(Alert.AlertType.WARNING, "Try Again!").show();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    private void loadAllProducts(String searchText) {
        try {
            List<Product> products = productService.findAllProducts();
            System.out.println("Loading products, found: " + products.size());

            ObservableList<ProductTm> tms = FXCollections.observableArrayList();

            // Filter products to show only those with low stock or out of stock batches if flag is set
            if (showLowStockOnly) {
                products = products.stream()
                    .filter(product -> {
                        List<ProductDetail> batches = productDetailService.findByProductCode(product.getCode());
                        return batches.stream().anyMatch(pd -> {
                            // Include low stock items (qty > 0 but <= threshold)
                            if (pd.isLowStock()) {
                                return true;
                            }
                            // Include out of stock items (qty <= 0)
                            if (pd.getQtyOnHand() <= 0) {
                                return true;
                            }
                            return false;
                        });
                    })
                    .collect(java.util.stream.Collectors.toList());
                System.out.println("Filtered to products with low stock or out of stock: " + products.size());
            }

            for (Product product : products) {
                // Create buttons for each row
                Button viewBarcode = new Button("View");
                Button delete = new Button("Delete");

                // Style buttons
                viewBarcode.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 15; -fx-font-size: 12px; -fx-background-radius: 4;");
                delete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 15; -fx-font-size: 12px; -fx-background-radius: 4;");

                String categoryName = product.getCategory() != null ? product.getCategory().getName() : "Uncategorized";
                String barcode = product.getBarcode() != null ? product.getBarcode() : "N/A";

                ProductTm tm = new ProductTm(
                        product.getCode(),
                        barcode,
                        product.getDescription(),
                        categoryName,
                        viewBarcode,
                        null, // showMore button removed
                        delete
                );
                tms.add(tm);

                // Add click handlers - barcode viewer moved to Feature/scanner branch
                viewBarcode.setOnAction((e) -> {
                    new Alert(Alert.AlertType.INFORMATION, "Barcode viewer feature has been moved to Feature/scanner branch").show();
                });
                
                delete.setOnAction((e) -> {
                    try {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                                "Are you sure you want to delete this product?\n\nBarcode: " + barcode + "\nDescription: " + product.getDescription(),
                                ButtonType.YES, ButtonType.NO);
                        alert.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.YES) {
                                if (productService.deleteProduct(product.getCode())) {
                                    new Alert(Alert.AlertType.INFORMATION, "Product deleted successfully!").show();
                                    loadAllProducts(searchText);
                                } else {
                                    new Alert(Alert.AlertType.WARNING, "Failed to delete product!").show();
                                }
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, "Error deleting product: " + ex.getMessage()).show();
                    }
                });
            }

            // Store all products in observable list
            allProducts = FXCollections.observableArrayList(tms);
            
            // Create filtered list
            filteredProducts = new FilteredList<>(allProducts, p -> true);
            
            // Set filtered list to table
            tbl.setItems(filteredProducts);
            
            // Reapply filter if search text exists
            if (txtSearchProducts != null && txtSearchProducts.getText() != null && !txtSearchProducts.getText().trim().isEmpty()) {
                filterProducts(txtSearchProducts.getText());
            }
            
            tbl.refresh();
            System.out.println("Product table updated with " + tms.size() + " items");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading products: " + e.getMessage());
            new Alert(Alert.AlertType.ERROR, "Error loading products: " + e.getMessage()).show();
        }
    }

    private void clearFields() {
        txtProductDescription.clear();
        cmbCategory.setValue(null);
        if (imgBarcode != null) {
            imgBarcode.setImage(null);
        }
        if (barcodeImageContainer != null) {
            barcodeImageContainer.setVisible(false);
            barcodeImageContainer.setManaged(false);
        }
        currentProductCode = null;
    }

    public void btnAddNewOnAction(ActionEvent actionEvent) {
        clearFields();
        btnSaveUpdate.setText("Save Product");
    }

    public void newBatchOnAction(ActionEvent actionEvent) throws IOException {
        loadExternalUi(false, null);
    }

    private void loadExternalUi(boolean state, ProductDetailTm tm) throws IOException {
        if (!txtSelectedProdId.getText().isEmpty()) {
            Stage stage = new Stage();

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/devstack/pos/view/NewBatchForm.fxml"));

            // Create controller instance manually (without Spring proxy) to avoid CGLIB issues
            // Then manually inject Spring services
            NewBatchFormController controller = new NewBatchFormController(
                    productDetailService,
                    productService,
                    supplierService
            );

            // Set controller BEFORE loading FXML
            loader.setController(controller);

            Parent parent = loader.load();

            // Set scene first
            Scene scene = new Scene(parent);
            stage.setScene(scene);
            
            // Configure stage for responsive resizing
            stage.setResizable(true);
            stage.setMinWidth(900.0);  // Minimum width to accommodate both panels
            stage.setMinHeight(600.0); // Minimum height for content
            stage.setWidth(1200.0);    // Default width
            stage.setHeight(800.0);    // Default height

            // Call setDetails after scene is set (initialize() will be called automatically by FXML)
            // Use Platform.runLater to ensure FXML injection is complete
            javafx.application.Platform.runLater(() -> {
                try {
                    int productCode = Integer.parseInt(txtSelectedProdId.getText());
                    controller.setDetails(productCode,
                            txtSelectedProdDescription.getText(), stage, state, tm);

                    // Refresh batch table when window closes
                    stage.setOnHidden(e -> {
                        if (currentProductCode != null) {
                            loadBatchData(currentProductCode);
                        }
                    });

            stage.show();
            stage.centerOnScreen();
                } catch (Exception e) {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Error initializing batch form: " + e.getMessage()).show();
                    stage.close();
                }
            });
        } else {
            new Alert(Alert.AlertType.WARNING, "Please select a valid one!").show();
        }
    }

    private void loadBatchData(int code) {
        if (tblDetail == null) return;

        try {
            ObservableList<ProductDetailTm> obList = FXCollections.observableArrayList();
            Product product = productService.findProduct(code);
            String productBarcode = product != null && product.getBarcode() != null ? product.getBarcode() : "";
            String productDescription = product != null ? product.getDescription() : "";

            // Get batches - filter to low stock and out of stock only if flag is set
            List<ProductDetail> batches = productDetailService.findByProductCode(code);
            if (showLowStockOnly) {
                batches = batches.stream()
                    .filter(pd -> {
                        // Include low stock items (qty > 0 but <= threshold)
                        if (pd.isLowStock()) {
                            return true;
                        }
                        // Include out of stock items (qty <= 0)
                        if (pd.getQtyOnHand() <= 0) {
                            return true;
                        }
                        return false;
                    })
                    .collect(java.util.stream.Collectors.toList());
            }

            for (ProductDetail productDetail : batches) {
                Button btnViewBarcode = new Button("View");
                Button btnDelete = new Button("Delete");

                // Style buttons
                btnViewBarcode.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 12; -fx-font-size: 11px; -fx-background-radius: 4;");
                btnDelete.setText("Delete");
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 12; -fx-font-size: 11px; -fx-background-radius: 4; -fx-font-weight: bold;");

                ProductDetailTm tm = new ProductDetailTm(
                        productDetail.getCode(),
                        productDetail.getQtyOnHand(),
                        productDetail.getSellingPrice(),
                        productDetail.getBuyingPrice(),
                        productDetail.isDiscountAvailability(),
                        // productDetail.getShowPrice(), // Commented out - Show Price logic removed
                        productDetail.getSupplierName() != null ? productDetail.getSupplierName() : "",
                        btnViewBarcode,
                        btnDelete
                );
                obList.add(tm);
                
                // View barcode button action - barcode viewer moved to Feature/scanner branch
                btnViewBarcode.setOnAction((e) -> {
                    new Alert(Alert.AlertType.INFORMATION, "Barcode viewer feature has been moved to Feature/scanner branch").show();
                });

                // Delete button action
                btnDelete.setOnAction((e) -> {
                    try {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                                "Are you sure you want to delete this batch?", ButtonType.YES, ButtonType.NO);
                        alert.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.YES) {
                                if (productDetailService.deleteProductDetail(productDetail.getCode())) {
                                    new Alert(Alert.AlertType.INFORMATION, "Batch deleted successfully!").show();
                                    loadBatchData(code);
                                } else {
                                    new Alert(Alert.AlertType.WARNING, "Failed to delete batch!").show();
                                }
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, ex.getMessage()).show();
                    }
                });
            }
            tblDetail.setItems(obList);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading batch data: " + e.getMessage()).show();
        }
    }


    // Navigation methods inherited from BaseController

    public void btnSaveProductOnAction(ActionEvent actionEvent) {
    }

    public void btnNewBatchOnAction(ActionEvent actionEvent) {
    }

    /**
     * Opens category management form
     */
    public void btnManageCategoriesOnAction(ActionEvent actionEvent) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/devstack/pos/view/CategoryManagementForm.fxml"));
            loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
            Parent parent = loader.load();
            stage.setScene(new Scene(parent));
            stage.setTitle("Category Management");
            stage.show();
            stage.centerOnScreen();

            // Reload categories when window closes
            stage.setOnHidden(e -> loadCategories());
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error opening category management: " + e.getMessage()).show();
        }
    }



    /**
     * Loads existing product data into the form
     */
    private void loadExistingProduct(Product product) {
        currentProductCode = product.getCode();
        txtProductDescription.setText(product.getDescription());
        if (product.getCategory() != null) {
            cmbCategory.setValue(product.getCategory().getName());
        }
        btnSaveUpdate.setText("Update Product");
        
        // Find and select the product in the table to update "Selected Product" section
        if (tbl != null && tbl.getItems() != null) {
            for (ProductTm tm : tbl.getItems()) {
                if (tm.getCode() == product.getCode()) {
                    // Select the product in the table, which will trigger setData() and update the selected product section
                    javafx.application.Platform.runLater(() -> {
                        tbl.getSelectionModel().select(tm);
                        tbl.scrollTo(tm);
                    });
                    break;
                }
            }
        }
    }

    /**
     * Barcode generation feature moved to Feature/scanner branch
     */
    public void btnGenerateBarcodeOnAction(ActionEvent actionEvent) {
        new Alert(Alert.AlertType.INFORMATION, "Barcode generation feature has been moved to Feature/scanner branch").show();
    }

    /**
     * Clears barcode image display
     */
    public void btnClearBarcodeOnAction(ActionEvent actionEvent) {
        if (imgBarcode != null) {
            imgBarcode.setImage(null);
        }
        if (barcodeImageContainer != null) {
            barcodeImageContainer.setVisible(false);
            barcodeImageContainer.setManaged(false);
        }
    }

    /**
     * Filters the product list based on search text
     * Searches by barcode, description, or category
     */
    private void filterProducts(String searchText) {
        if (filteredProducts == null) {
            return;
        }

        if (searchText == null || searchText.trim().isEmpty()) {
            // Show all products if search is empty
            filteredProducts.setPredicate(product -> true);
        } else {
            // Filter products based on search text (case-insensitive)
            String lowerSearchText = searchText.toLowerCase().trim();
            filteredProducts.setPredicate(product -> {
                // Search in barcode
                if (product.getBarcode() != null && 
                    product.getBarcode().toLowerCase().contains(lowerSearchText)) {
                    return true;
                }
                // Search in description
                if (product.getDescription() != null && 
                    product.getDescription().toLowerCase().contains(lowerSearchText)) {
                    return true;
                }
                // Search in category
                if (product.getCategory() != null && 
                    product.getCategory().toLowerCase().contains(lowerSearchText)) {
                    return true;
                }
                return false;
            });
        }
    }
}
