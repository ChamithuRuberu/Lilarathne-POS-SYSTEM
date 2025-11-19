package com.devstack.pos.controller;

import com.devstack.pos.entity.Category;
import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.service.CategoryService;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.ProductService;
import com.devstack.pos.service.SupplierService;
import com.devstack.pos.util.BarcodeGenerator;
import com.devstack.pos.view.tm.ProductDetailTm;
import com.devstack.pos.view.tm.ProductTm;
import com.google.zxing.WriterException;
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
    public TextField txtBarcode;
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
    private Timeline barcodeDebounceTimeline;
    private boolean isUpdatingBarcodeProgrammatically = false;
    private long lastBarcodeInputTime = 0;
    private int barcodeInputChangeCount = 0;
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
        colPDQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
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

        // Setup barcode scanner listener - when user presses Enter in barcode field
        txtBarcode.setOnAction(event -> {
            System.out.println("[BARCODE DEBUG] Enter key pressed - triggering handleBarcodeInput()");
            handleBarcodeInput();
        });
        
        // Also listen for key events as backup (some scanners send Tab or other keys)
        txtBarcode.setOnKeyPressed(event -> {
            System.out.println("[BARCODE DEBUG] ========== KEY PRESSED ==========");
            System.out.println("[BARCODE DEBUG] Key code: " + event.getCode());
            System.out.println("[BARCODE DEBUG] Key text: " + event.getText());
            System.out.println("[BARCODE DEBUG] Current field text: '" + txtBarcode.getText() + "'");
        });
        
        // Listen for key typed events (captures actual characters)
        txtBarcode.setOnKeyTyped(event -> {
            System.out.println("[BARCODE DEBUG] ========== KEY TYPED ==========");
            System.out.println("[BARCODE DEBUG] Character: '" + event.getCharacter() + "'");
            System.out.println("[BARCODE DEBUG] Current field text: '" + txtBarcode.getText() + "'");
        });
        
        // Listen for input method events (some scanners use input methods)
        txtBarcode.setOnInputMethodTextChanged(event -> {
            System.out.println("[BARCODE DEBUG] ========== INPUT METHOD TEXT CHANGED ==========");
            System.out.println("[BARCODE DEBUG] Committed text: '" + event.getCommitted() + "'");
            System.out.println("[BARCODE DEBUG] Current field text: '" + txtBarcode.getText() + "'");
        });
        
        // Ensure field can receive input from barcode scanners
        javafx.application.Platform.runLater(() -> {
            // Request focus when page loads to make it ready for scanning
            txtBarcode.requestFocus();
            System.out.println("[BARCODE DEBUG] Barcode field initialized and focused");
            System.out.println("[BARCODE DEBUG] Field is editable: " + txtBarcode.isEditable());
            System.out.println("[BARCODE DEBUG] Field is disabled: " + txtBarcode.isDisabled());
            System.out.println("[BARCODE DEBUG] Field is visible: " + txtBarcode.isVisible());
            System.out.println("[BARCODE DEBUG] Field has focus: " + txtBarcode.isFocused());
            System.out.println("[BARCODE DEBUG] Field is managed: " + txtBarcode.isManaged());
            
            // Add a focus listener to track focus changes
            txtBarcode.focusedProperty().addListener((observable, oldValue, newValue) -> {
                System.out.println("[BARCODE DEBUG] Focus changed: " + oldValue + " -> " + newValue);
            });
            
            // Verify listeners are attached
            System.out.println("[BARCODE DEBUG] All listeners attached and ready for barcode input");
            System.out.println("[BARCODE DEBUG] ==========================================");
            System.out.println("[BARCODE DEBUG] TEST: Try typing manually in the barcode field to verify listeners work");
            System.out.println("[BARCODE DEBUG] Then try scanning a barcode...");
            System.out.println("[BARCODE DEBUG] ==========================================");
            
            // Test if manual typing works (to verify listeners are functioning)
            // This will help us determine if the issue is with the scanner or the listeners
            System.out.println("[BARCODE DEBUG] If you see this message but NO events when typing/scanning,");
            System.out.println("[BARCODE DEBUG] the field might not be receiving input. Check:");
            System.out.println("[BARCODE DEBUG] 1. Is the barcode field visible on screen?");
            System.out.println("[BARCODE DEBUG] 2. Is the barcode scanner configured correctly?");
            System.out.println("[BARCODE DEBUG] 3. Does the scanner send data as keyboard input?");
        });
        
        // Setup debounced text change listener for barcode scanners
        // Barcode scanners typically send data very quickly (all at once), so we use a delay
        // to ensure we capture the complete barcode before processing
        // Reduced delay to 200ms for faster response while still capturing complete barcode
        barcodeDebounceTimeline = new Timeline(new KeyFrame(Duration.millis(200), e -> {
            System.out.println("[BARCODE DEBUG] Debounce timer fired");
            // Don't trigger if we're updating the barcode programmatically
            if (isUpdatingBarcodeProgrammatically) {
                System.out.println("[BARCODE DEBUG] Skipping - updating programmatically");
                return;
            }
            String barcode = txtBarcode.getText();
            System.out.println("[BARCODE DEBUG] Current barcode text: '" + barcode + "' (length: " + (barcode != null ? barcode.length() : 0) + ")");
            if (barcode != null && !barcode.trim().isEmpty()) {
                // Remove any newline/carriage return characters that barcode scanners might append
                String originalBarcode = barcode;
                barcode = barcode.replace("\n", "").replace("\r", "").trim();
                if (!barcode.equals(originalBarcode)) {
                    System.out.println("[BARCODE DEBUG] Removed newline characters. Original: '" + originalBarcode + "', Cleaned: '" + barcode + "'");
                }
                if (!barcode.isEmpty()) {
                    // SIMPLIFIED: Always process barcode after debounce delay (300ms)
                    // This ensures we capture complete barcode from scanner
                    // The debounce delay itself is enough to distinguish scanner input from manual typing
                    System.out.println("[BARCODE DEBUG] ✓ Processing barcode automatically!");
                    System.out.println("[BARCODE DEBUG] Detection info - Change count: " + barcodeInputChangeCount + ", Has focus: " + txtBarcode.isFocused() + ", Length: " + barcode.length());
                    
                    // Update the text field to remove newlines if any
                    // This ensures the barcode field always contains the clean value
                    String currentText = txtBarcode.getText().replace("\n", "").replace("\r", "").trim();
                    if (!currentText.equals(txtBarcode.getText())) {
                        System.out.println("[BARCODE DEBUG] Cleaning text field - removing newlines");
                        System.out.println("[BARCODE DEBUG] Original: '" + txtBarcode.getText() + "', Cleaned: '" + currentText + "'");
                        isUpdatingBarcodeProgrammatically = true;
                        try {
                            txtBarcode.setText(currentText);
                            System.out.println("[BARCODE DEBUG] Barcode field updated with clean value: '" + currentText + "'");
                        } finally {
                            javafx.application.Platform.runLater(() -> {
                                javafx.application.Platform.runLater(() -> {
                                    isUpdatingBarcodeProgrammatically = false;
                                });
                            });
                        }
                    } else {
                        System.out.println("[BARCODE DEBUG] Barcode field already clean: '" + currentText + "'");
                    }
                    // Reset counter
                    barcodeInputChangeCount = 0;
                    System.out.println("[BARCODE DEBUG] Calling handleBarcodeInput() with barcode: '" + barcode + "'");
                    handleBarcodeInput();
                } else {
                    System.out.println("[BARCODE DEBUG] Barcode is empty after cleaning");
                }
            } else {
                System.out.println("[BARCODE DEBUG] Barcode is null or empty");
            }
        }));
        barcodeDebounceTimeline.setCycleCount(1);
        
        // Add a more comprehensive listener that will definitely catch scanner input
        txtBarcode.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("[BARCODE DEBUG] ========== TEXT PROPERTY CHANGED ==========");
            System.out.println("[BARCODE DEBUG] Old value: '" + (oldValue != null ? oldValue : "null") + "'");
            System.out.println("[BARCODE DEBUG] New value: '" + (newValue != null ? newValue : "null") + "'");
            System.out.println("[BARCODE DEBUG] Is updating programmatically: " + isUpdatingBarcodeProgrammatically);
            
            // Don't start debounce timer if we're updating programmatically
            if (isUpdatingBarcodeProgrammatically) {
                System.out.println("[BARCODE DEBUG] Skipping - updating programmatically");
                return;
            }
            
            System.out.println("[BARCODE DEBUG] Processing text change...");
            
            // Track input changes to detect barcode scanner (rapid input = many changes quickly)
            long currentTime = System.currentTimeMillis();
            long timeSinceLastInput = currentTime - lastBarcodeInputTime;
            if (timeSinceLastInput < 100) {
                // Rapid input detected (changes within 100ms) - likely barcode scanner
                barcodeInputChangeCount++;
                System.out.println("[BARCODE DEBUG] Rapid input detected! Time since last: " + timeSinceLastInput + "ms, Change count: " + barcodeInputChangeCount);
            } else {
                // Reset counter if input slowed down
                barcodeInputChangeCount = 1;
                System.out.println("[BARCODE DEBUG] Normal input speed. Time since last: " + timeSinceLastInput + "ms, Reset count to: " + barcodeInputChangeCount);
            }
            lastBarcodeInputTime = currentTime;
            
            // Reset and restart the debounce timer when text changes
            if (barcodeDebounceTimeline != null) {
                System.out.println("[BARCODE DEBUG] Restarting debounce timer (200ms delay)");
                barcodeDebounceTimeline.stop();
                barcodeDebounceTimeline.playFromStart();
            }
        });

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
            // Get barcode value directly from field - ensure we get the actual current value
            String rawBarcode = txtBarcode.getText();
            System.out.println("[BARCODE DEBUG] ========== SAVE PRODUCT ==========");
            System.out.println("[BARCODE DEBUG] Raw barcode from field: '" + rawBarcode + "'");
            System.out.println("[BARCODE DEBUG] Raw barcode length: " + (rawBarcode != null ? rawBarcode.length() : 0));
            
            // Clean barcode value (remove any newlines/carriage returns)
            String barcodeValue = rawBarcode;
            if (barcodeValue != null) {
                String beforeClean = barcodeValue;
                barcodeValue = barcodeValue.replace("\n", "").replace("\r", "").trim();
                if (!barcodeValue.equals(beforeClean)) {
                    System.out.println("[BARCODE DEBUG] Cleaned barcode - Before: '" + beforeClean + "', After: '" + barcodeValue + "'");
                }
            } else {
                barcodeValue = "";
            }
            
            System.out.println("[BARCODE DEBUG] Final barcode value to save: '" + barcodeValue + "'");
            System.out.println("[BARCODE DEBUG] Barcode value length: " + barcodeValue.length());
            
            // Validate inputs
            if (barcodeValue.isEmpty()) {
                System.out.println("[BARCODE DEBUG] ERROR: Barcode is empty!");
                new Alert(Alert.AlertType.WARNING, "Please enter or scan a barcode!").show();
                return;
            }

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
            product.setBarcode(barcodeValue); // Use cleaned barcode value
            
            System.out.println("[BARCODE DEBUG] Product object barcode set to: '" + product.getBarcode() + "'");
            System.out.println("[BARCODE DEBUG] About to call productService.saveProduct()");

            // Set category
            Category category = categoryService.findCategoryByName(cmbCategory.getValue());
            if (category != null) {
                product.setCategory(category);
            }

            if (btnSaveUpdate.getText().equals("Save Product")) {
                System.out.println("[BARCODE DEBUG] Calling productService.saveProduct()");
                System.out.println("[BARCODE DEBUG] Product barcode before save: '" + product.getBarcode() + "'");
                Product savedProduct = productService.saveProduct(product);
                System.out.println("[BARCODE DEBUG] Product saved successfully!");
                System.out.println("[BARCODE DEBUG] Saved product barcode: '" + (savedProduct.getBarcode() != null ? savedProduct.getBarcode() : "NULL") + "'");
                System.out.println("[BARCODE DEBUG] Saved product code: " + savedProduct.getCode());
                
                new Alert(Alert.AlertType.CONFIRMATION,
                        "Product Saved Successfully!\nBarcode: " + savedProduct.getBarcode()).show();

                // Display the generated barcode
                if (savedProduct.getBarcode() != null) {
                    displayBarcode(savedProduct.getBarcode());
                }

                clearFields();
                loadAllProducts(searchText);
            } else {
                // Update existing product
                System.out.println("[BARCODE DEBUG] Updating existing product (Code: " + currentProductCode + ")");
                System.out.println("[BARCODE DEBUG] Product barcode before update: '" + product.getBarcode() + "'");
                if (currentProductCode != null) {
                    product.setCode(currentProductCode);
                if (productService.updateProduct(product)) {
                    System.out.println("[BARCODE DEBUG] Product updated successfully!");
                    new Alert(Alert.AlertType.CONFIRMATION, "Product Updated!").show();
                    clearFields();
                    loadAllProducts(searchText);
                    btnSaveUpdate.setText("Save Product");
                } else {
                    System.out.println("[BARCODE DEBUG] Product update failed!");
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

            // Filter products to show only those with low stock batches if flag is set
            if (showLowStockOnly) {
                products = products.stream()
                    .filter(product -> {
                        List<ProductDetail> batches = productDetailService.findByProductCode(product.getCode());
                        return batches.stream().anyMatch(ProductDetail::isLowStock);
                    })
                    .collect(java.util.stream.Collectors.toList());
                System.out.println("Filtered to products with low stock: " + products.size());
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

                // Add click handlers
                viewBarcode.setOnAction((e) -> {
                    if (product.getBarcode() != null && !product.getBarcode().isEmpty()) {
                        showBarcodeViewer(product.getCode(), product.getDescription(), product.getBarcode());
                    } else {
                        new Alert(Alert.AlertType.WARNING, "This product does not have a barcode!").show();
                    }
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
        // Set flag to prevent debounce timer from triggering during programmatic clear
        isUpdatingBarcodeProgrammatically = true;
        try {
            txtProductDescription.clear();
            txtBarcode.clear();
            cmbCategory.setValue(null);
            imgBarcode.setImage(null);
            barcodeImageContainer.setVisible(false);
            barcodeImageContainer.setManaged(false);
            currentProductCode = null;
        } finally {
            // Reset flag after a short delay
            javafx.application.Platform.runLater(() -> {
                javafx.application.Platform.runLater(() -> {
                    isUpdatingBarcodeProgrammatically = false;
                });
            });
        }
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
            stage.setScene(new Scene(parent));

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

            // Get batches - filter to low stock only if flag is set
            List<ProductDetail> batches = productDetailService.findByProductCode(code);
            if (showLowStockOnly) {
                batches = batches.stream()
                    .filter(ProductDetail::isLowStock)
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
                
                // View barcode button action
                btnViewBarcode.setOnAction((e) -> {
                    try {
                        // Decode batch barcode from Base64 if available
                        String batchBarcode = productDetail.getBarcode();
                        if (batchBarcode != null && !batchBarcode.isEmpty()) {
                            try {
                                // Try to decode as Base64 image
                                byte[] imageData = java.util.Base64.getDecoder().decode(batchBarcode);
                                Image batchImage = new Image(new ByteArrayInputStream(imageData));
                                showBatchBarcodeViewerWithImage(code, productDescription, productDetail.getCode(), batchImage);
                            } catch (Exception ex) {
                                // If not Base64, treat as barcode value and generate image
                                showBatchBarcodeViewer(code, productDescription, productDetail.getCode(), batchBarcode);
                            }
                        } else if (!productBarcode.isEmpty()) {
                            // Fallback to product barcode if batch barcode not available
                            showBarcodeViewer(code, productDescription, productBarcode);
                        } else {
                            new Alert(Alert.AlertType.WARNING, "No barcode available for this batch!").show();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, "Error viewing barcode: " + ex.getMessage()).show();
                    }
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

    /**
     * Shows batch barcode viewer dialog with download option (for barcode string)
     */
    private void showBatchBarcodeViewer(int productCode, String productDescription, String batchCode, String batchBarcode) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/devstack/pos/view/BarcodeViewerForm.fxml"));
            loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
            Parent parent = loader.load();
            BarcodeViewerController controller = loader.getController();

            // Set stage first
            controller.setStage(stage);
            
            // Set scene and show stage first
            stage.setScene(new Scene(parent));
            stage.setTitle("Batch Barcode Viewer");
            stage.show();
            stage.centerOnScreen();
            
            // Use Platform.runLater to ensure FXML fields are fully initialized after scene is shown
            javafx.application.Platform.runLater(() -> {
                // Use batch code as barcode value for display
                controller.setData(productCode, productDescription + " (Batch: " + batchCode + ")", batchCode);
            });
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error opening barcode viewer: " + e.getMessage()).show();
        }
    }

    /**
     * Shows batch barcode viewer dialog with pre-loaded image (for Base64 encoded images)
     */
    private void showBatchBarcodeViewerWithImage(int productCode, String productDescription, String batchCode, Image batchImage) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/devstack/pos/view/BarcodeViewerForm.fxml"));
            loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
            Parent parent = loader.load();
            BarcodeViewerController controller = loader.getController();

            // Set stage first
            controller.setStage(stage);
            
            // Set scene and show stage first
            stage.setScene(new Scene(parent));
            stage.setTitle("Batch Barcode Viewer");
            stage.show();
            stage.centerOnScreen();
            
            // Use Platform.runLater to ensure FXML fields are fully initialized after scene is shown
            javafx.application.Platform.runLater(() -> {
                // Use batch code as barcode value, with pre-loaded image
                controller.setDataWithImage(productCode, productDescription + " (Batch: " + batchCode + ")", batchCode, batchImage);
            });
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error opening barcode viewer: " + e.getMessage()).show();
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
     * Shows barcode viewer dialog with download option
     */
    private void showBarcodeViewer(int productCode, String description, String barcode) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/devstack/pos/view/BarcodeViewerForm.fxml"));
            loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
            Parent parent = loader.load();
            BarcodeViewerController controller = loader.getController();
            
            // Set stage first
            controller.setStage(stage);
            
            // Set scene and show stage first
            stage.setScene(new Scene(parent));
            stage.setTitle("Product Barcode Viewer");
            stage.show();
            stage.centerOnScreen();
            
            // Use Platform.runLater to ensure FXML fields are fully initialized after scene is shown
            javafx.application.Platform.runLater(() -> {
                controller.setData(productCode, description, barcode);
            });
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error opening barcode viewer: " + e.getMessage()).show();
        }
    }

    /**
     * Handles barcode input from scanner or manual entry
     */
    private void handleBarcodeInput() {
        System.out.println("[BARCODE DEBUG] ========== handleBarcodeInput() called ==========");
        String barcode = txtBarcode.getText().trim();
        System.out.println("[BARCODE DEBUG] Barcode value: '" + barcode + "' (length: " + barcode.length() + ")");
        if (!barcode.isEmpty()) {
            System.out.println("[BARCODE DEBUG] Looking up product by barcode...");
            // Try to find existing product by barcode
            Product existingProduct = productService.findProductByBarcode(barcode);
            System.out.println("[BARCODE DEBUG] Product lookup result: " + (existingProduct != null ? "FOUND (Code: " + existingProduct.getCode() + ")" : "NOT FOUND"));

            if (existingProduct != null) {
                // Product exists - AUTO-LOAD without confirmation for better UX with barcode scanners
                System.out.println("[BARCODE DEBUG] Product found - auto-loading product data");
                loadExistingProduct(existingProduct);
            } else {
                // New barcode, validate and show preview
                if (BarcodeGenerator.isValidBarcode(barcode)) {
                    try {
                        displayBarcode(barcode);
                        new Alert(Alert.AlertType.INFORMATION,
                                "Barcode is valid! You can now enter product details and save.").show();
                    } catch (Exception e) {
                        new Alert(Alert.AlertType.ERROR,
                                "Error generating barcode preview: " + e.getMessage()).show();
                    }
                } else {
                    new Alert(Alert.AlertType.WARNING,
                            "Invalid barcode format. Use alphanumeric characters only.").show();
                }
            }
        }
    }

    /**
     * Loads existing product data into the form
     */
    private void loadExistingProduct(Product product) {
        // Set flag to prevent debounce timer from triggering during programmatic update
        isUpdatingBarcodeProgrammatically = true;
        
        try {
            currentProductCode = product.getCode();
            txtProductDescription.setText(product.getDescription());
            txtBarcode.setText(product.getBarcode());
            if (product.getCategory() != null) {
                cmbCategory.setValue(product.getCategory().getName());
            }
            btnSaveUpdate.setText("Update Product");

            if (product.getBarcode() != null) {
                displayBarcode(product.getBarcode());
            }
            
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
        } finally {
            // Reset flag after a short delay to allow the text change to complete
            javafx.application.Platform.runLater(() -> {
                javafx.application.Platform.runLater(() -> {
                    isUpdatingBarcodeProgrammatically = false;
                });
            });
        }
    }

    /**
     * Generates and displays barcode preview
     */
    public void btnGenerateBarcodeOnAction(ActionEvent actionEvent) {
        try {
            String barcodeValue = txtBarcode.getText().trim();

            // If no barcode entered, generate a random one
            if (barcodeValue.isEmpty()) {
                barcodeValue = BarcodeGenerator.generateNumeric(12);
                // Set flag to prevent debounce timer from triggering during programmatic set
                isUpdatingBarcodeProgrammatically = true;
                try {
                    txtBarcode.setText(barcodeValue);
                } finally {
                    javafx.application.Platform.runLater(() -> {
                        javafx.application.Platform.runLater(() -> {
                            isUpdatingBarcodeProgrammatically = false;
                        });
                    });
                }
            }

            // Validate barcode
            if (!BarcodeGenerator.isValidBarcode(barcodeValue)) {
                new Alert(Alert.AlertType.WARNING,
                        "Invalid barcode format. Use alphanumeric characters only.").show();
                return;
            }

            // Check if barcode already exists
            if (productService.barcodeExists(barcodeValue) &&
                    !btnSaveUpdate.getText().equals("Update Product")) {
                new Alert(Alert.AlertType.WARNING,
                        "This barcode already exists in the system!").show();
                return;
            }

            displayBarcode(barcodeValue);
            new Alert(Alert.AlertType.INFORMATION, "Barcode preview generated successfully!").show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error generating barcode: " + e.getMessage()).show();
        }
    }

    /**
     * Clears barcode field and image
     */
    public void btnClearBarcodeOnAction(ActionEvent actionEvent) {
        // Set flag to prevent debounce timer from triggering during programmatic clear
        isUpdatingBarcodeProgrammatically = true;
        try {
            txtBarcode.clear();
            imgBarcode.setImage(null);
            barcodeImageContainer.setVisible(false);
            barcodeImageContainer.setManaged(false);
        } finally {
            // Reset flag after a short delay
            javafx.application.Platform.runLater(() -> {
                javafx.application.Platform.runLater(() -> {
                    isUpdatingBarcodeProgrammatically = false;
                });
            });
        }
    }

    /**
     * Displays the barcode image
     */
    private void displayBarcode(String barcodeValue) {
        try {
            Image barcodeImage = BarcodeGenerator.generateBarcodeImage(barcodeValue);
            imgBarcode.setImage(barcodeImage);
            barcodeImageContainer.setVisible(true);
            barcodeImageContainer.setManaged(true);
        } catch (WriterException e) {
            throw new RuntimeException("Failed to generate barcode image", e);
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
