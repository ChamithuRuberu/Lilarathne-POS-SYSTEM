package com.devstack.pos.controller;

import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.entity.Supplier;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.ProductService;
import com.devstack.pos.service.SupplierService;
import com.devstack.pos.util.BarcodeGenerator;
import com.devstack.pos.view.tm.ProductDetailTm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class NewBatchFormController {

    @FXML
    public AnchorPane context;
    @FXML
    public ImageView barcodeImage;
    
    // Product Information (Read-only)
    @FXML
    public TextField txtProductCode;
    @FXML
    public TextField txtProductBarcode;
    @FXML
    public TextField txtBatchBarcodeCode; // Manual batch barcode code entry
    @FXML
    public TextArea txtSelectedProdDescription;
    
    // Pricing Fields
    @FXML
    public TextField txtQty;
    @FXML
    public TextField txtLowStockThreshold;
    @FXML
    public TextField txtBuyingPrice;
    @FXML
    public TextField txtSellingPrice;
    @FXML
    public TextField txtProfitMargin;
    
    // Batch Tracking Fields
    @FXML
    public TextField txtBatchNumber;
    @FXML
    public DatePicker dateManufacturing;
    @FXML
    public DatePicker dateExpiry;
    
    // Supplier Fields
    @FXML
    public ComboBox cmbSupplier;  // Raw type for FXML compatibility
    @FXML
    public TextField txtSupplierContact;
    
    String uniqueData = null;
    Stage stage = null;
    private Integer currentProductCode = null;
    private boolean isEditMode = false;
    private String existingBatchCode = null;

    private final ProductDetailService productDetailService;
    private final ProductService productService;
    private final SupplierService supplierService;

    @FXML
    public void initialize() {
        setupProfitMarginCalculation();
        setupDateValidation();
        setupNumericValidation();
        loadSuppliers();
        setupSupplierSelection();
        setupManualBarcodeEntry();
    }
    
    /**
     * Load suppliers into the combo box
     */
    @SuppressWarnings("unchecked")
    private void loadSuppliers() {
        try {
            if (cmbSupplier != null) {
                ObservableList<Supplier> suppliers = FXCollections.observableArrayList(
                    supplierService.findActiveSuppliers()
                );
                cmbSupplier.setItems(suppliers);
                
                // Set cell factory to display supplier name
                cmbSupplier.setCellFactory(param -> new ListCell<Supplier>() {
                    @Override
                    protected void updateItem(Supplier item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getName());
                        }
                    }
                });
                
                // Set button cell to display supplier name
                cmbSupplier.setButtonCell(new ListCell<Supplier>() {
                    @Override
                    protected void updateItem(Supplier item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getName());
                        }
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error loading suppliers: " + e.getMessage());
        }
    }
    
    /**
     * Setup supplier selection listener to auto-fill contact
     */
    @SuppressWarnings("unchecked")
    private void setupSupplierSelection() {
        if (cmbSupplier != null && txtSupplierContact != null) {
            cmbSupplier.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal instanceof Supplier) {
                    Supplier supplier = (Supplier) newVal;
                    // Auto-fill supplier contact (phone)
                    String contact = supplier.getPhone() != null ? supplier.getPhone() : "";
                    if (contact.isEmpty() && supplier.getEmail() != null) {
                        contact = supplier.getEmail();
                    }
                    txtSupplierContact.setText(contact);
                } else {
                    txtSupplierContact.clear();
                }
            });
        }
    }
    
    /**
     * Setup manual barcode entry listener to generate barcode from manual code
     */
    private void setupManualBarcodeEntry() {
        if (txtBatchBarcodeCode != null) {
            txtBatchBarcodeCode.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.trim().isEmpty()) {
                    // User entered manual code, generate barcode from it
                    try {
                        generateBarcodeFromCode(newVal.trim());
                    } catch (Exception e) {
                        System.err.println("Error generating barcode from manual code: " + e.getMessage());
                    }
                } else {
                    // Field is empty, regenerate auto barcode if not in edit mode
                    if (!isEditMode) {
                        try {
                            generateBatchBarcode();
                        } catch (Exception e) {
                            System.err.println("Error regenerating auto barcode: " + e.getMessage());
                        }
                    }
                }
            });
        }
    }
    
    /**
     * Generate barcode image from a given code string
     */
    private void generateBarcodeFromCode(String code) throws WriterException, IOException {
        if (code == null || code.trim().isEmpty()) {
            return;
        }
        
        // Use the provided code as uniqueData
        uniqueData = code.trim();
        
        // Ensure barcode is valid (alphanumeric, max length for CODE 128)
        if (uniqueData.length() > 80) {
            uniqueData = uniqueData.substring(0, 80);
        }
        
        // Generate CODE 128 barcode image
        Code128Writer barcodeWriter = new Code128Writer();
        BitMatrix bitMatrix = barcodeWriter.encode(
            uniqueData,
            BarcodeFormat.CODE_128,
            300,
            80
        );
        
        BufferedImage barcodeBufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        Image image = SwingFXUtils.toFXImage(barcodeBufferedImage, null);
        
        if (barcodeImage != null) {
            barcodeImage.setImage(image);
        }
    }

    /**
     * Auto-calculate profit margin when prices change
     */
    private void setupProfitMarginCalculation() {
        if (txtBuyingPrice != null && txtSellingPrice != null && txtProfitMargin != null) {
            txtBuyingPrice.textProperty().addListener((obs, oldVal, newVal) -> calculateProfitMargin());
            txtSellingPrice.textProperty().addListener((obs, oldVal, newVal) -> calculateProfitMargin());
        }
    }

    /**
     * Calculate and display profit margin
     */
    private void calculateProfitMargin() {
        try {
            if (txtBuyingPrice.getText() != null && !txtBuyingPrice.getText().trim().isEmpty() &&
                txtSellingPrice.getText() != null && !txtSellingPrice.getText().trim().isEmpty()) {
                
                double buyingPrice = Double.parseDouble(txtBuyingPrice.getText().trim());
                double sellingPrice = Double.parseDouble(txtSellingPrice.getText().trim());
                
                if (buyingPrice > 0) {
                    double profitMargin = ((sellingPrice - buyingPrice) / buyingPrice) * 100;
                    txtProfitMargin.setText(String.format("%.2f%%", profitMargin));
                    
                    // Color code based on profit margin
                    if (profitMargin < 10) {
                        txtProfitMargin.setStyle("-fx-text-fill: red; -fx-background-color: #ffe6e6;");
                    } else if (profitMargin < 20) {
                        txtProfitMargin.setStyle("-fx-text-fill: orange; -fx-background-color: #fff3e6;");
                    } else {
                        txtProfitMargin.setStyle("-fx-text-fill: green; -fx-background-color: #e6ffe6;");
                    }
                }
            }
        } catch (NumberFormatException e) {
            txtProfitMargin.setText("0.00%");
            txtProfitMargin.setStyle("-fx-background-color: #f0f0f0;");
        }
    }

    /**
     * Setup validation for dates
     */
    private void setupDateValidation() {
        if (dateExpiry != null && dateManufacturing != null) {
            dateExpiry.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && dateManufacturing.getValue() != null) {
                    if (newVal.isBefore(dateManufacturing.getValue()) || newVal.isEqual(dateManufacturing.getValue())) {
                        dateExpiry.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    } else {
                        dateExpiry.setStyle("");
                    }
                }
            });
            
            dateManufacturing.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && dateExpiry.getValue() != null) {
                    if (dateExpiry.getValue().isBefore(newVal) || dateExpiry.getValue().isEqual(newVal)) {
                        dateManufacturing.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    } else {
                        dateManufacturing.setStyle("");
                    }
                }
            });
        }
    }

    /**
     * Setup numeric validation for input fields
     */
    private void setupNumericValidation() {
        // Quantity - only integers
        if (txtQty != null) {
            txtQty.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    txtQty.setText(oldVal);
                }
            });
        }
        
        // Low stock threshold - only integers
        if (txtLowStockThreshold != null) {
            txtLowStockThreshold.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    txtLowStockThreshold.setText(oldVal);
                }
            });
        }
        
        // Prices - allow decimals
        setupDecimalField(txtBuyingPrice);
        setupDecimalField(txtSellingPrice);
    }

    /**
     * Setup decimal validation for a field
     */
    private void setupDecimalField(TextField field) {
        if (field != null) {
            field.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*\\.?\\d*")) {
                    field.setText(oldVal);
                }
            });
        }
    }

    /**
     * Generate short form from product description
     * Takes first letter of each word (up to 4 words) and converts to uppercase
     */
    private String generateShortForm(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "PROD";
        }
        
        // Clean and split description into words
        String[] words = description.trim().toUpperCase()
                .replaceAll("[^A-Z0-9\\s]", "") // Remove special characters, keep alphanumeric
                .split("\\s+");
        
        StringBuilder shortForm = new StringBuilder();
        
        // Take first letter of each word, up to 4 words
        int maxWords = Math.min(words.length, 4);
        for (int i = 0; i < maxWords; i++) {
            if (!words[i].isEmpty()) {
                // Take first character, or first 2 if it's a number
                char firstChar = words[i].charAt(0);
                if (Character.isLetter(firstChar)) {
                    shortForm.append(firstChar);
                } else if (Character.isDigit(firstChar)) {
                    // For numbers, take first 2 digits if available
                    shortForm.append(words[i].substring(0, Math.min(2, words[i].length())));
                }
            }
        }
        
        // If we got nothing, use first 4 uppercase letters/numbers from description
        if (shortForm.length() == 0) {
            String cleaned = description.toUpperCase().replaceAll("[^A-Z0-9]", "");
            shortForm.append(cleaned.substring(0, Math.min(4, cleaned.length())));
        }
        
        // Ensure minimum length of 2
        if (shortForm.length() < 2) {
            shortForm.append("XX");
        }
        
        return shortForm.toString();
    }

    /**
     * Generate unique batch barcode with meaningful short form from product description
     */
    private void generateBatchBarcode() throws WriterException, IOException {
        // Get product description
        String description = txtSelectedProdDescription != null ? 
                txtSelectedProdDescription.getText() : "";
        
        // Generate short form from description
        String shortForm = generateShortForm(description);
        
        // Generate unique numeric suffix (6 digits for uniqueness)
        String numericSuffix = BarcodeGenerator.generateNumeric(6);
        
        // Combine short form with numeric suffix
        // Format: SHORTFORM + 6 digits (e.g., "COKE123456")
        uniqueData = shortForm + numericSuffix;
        
        // Ensure barcode is valid (alphanumeric, max length for CODE 128)
        // CODE 128 supports up to 80 characters, but we'll keep it reasonable
        if (uniqueData.length() > 20) {
            uniqueData = uniqueData.substring(0, 20);
        }
        
        // Generate CODE 128 barcode image
        Code128Writer barcodeWriter = new Code128Writer();
        BitMatrix bitMatrix = barcodeWriter.encode(
                                uniqueData,
                BarcodeFormat.CODE_128,
            300,
            80
        );
        
        BufferedImage barcodeBufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        Image image = SwingFXUtils.toFXImage(barcodeBufferedImage, null);
        
        if (barcodeImage != null) {
            barcodeImage.setImage(image);
        }
    }

    /**
     * Generate batch number based on product code and timestamp
     */
    private String generateBatchNumber() {
        if (currentProductCode == null) return null;
        
        String dateStamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomSuffix = (int) (Math.random() * 1000);
        return String.format("B%d-%s-%03d", currentProductCode, dateStamp, randomSuffix);
    }

    /**
     * Set batch details for new or edit mode
     */
    public void setDetails(int code, String description, Stage stage, boolean isEdit, ProductDetailTm tm) {
        this.stage = stage;
        this.currentProductCode = code;
        this.isEditMode = isEdit;
        
        if (!isEdit) {
            resetFormFields();
        }

        // Always show basic product info using provided parameters
        if (txtProductCode != null) {
            txtProductCode.setText(String.valueOf(code));
        }
        
        if (txtSelectedProdDescription != null) {
            txtSelectedProdDescription.setText(description != null ? description : "");
        }

        // Fetch product details
        try {
            Product product = productService.findProduct(code);
            if (product != null) {
                // Set read-only product information
                if (txtProductBarcode != null && product.getBarcode() != null) {
                    txtProductBarcode.setText(product.getBarcode());
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching product details: " + e.getMessage());
        }

        if (isEdit && tm != null) {
            // EDIT MODE - Load existing batch data
            try {
                ProductDetail productDetail = productDetailService.findProductDetail(tm.getCode());
                if (productDetail != null) {
                    existingBatchCode = productDetail.getCode();
                    
                    // Load existing data
                    if (txtQty != null) txtQty.setText(String.valueOf(productDetail.getQtyOnHand()));
                    if (txtLowStockThreshold != null && productDetail.getLowStockThreshold() != null) {
                        txtLowStockThreshold.setText(String.valueOf(productDetail.getLowStockThreshold()));
                    }
                    if (txtBuyingPrice != null) txtBuyingPrice.setText(String.format("%.2f", productDetail.getBuyingPrice()));
                    if (txtSellingPrice != null) txtSellingPrice.setText(String.format("%.2f", productDetail.getSellingPrice()));
                    if (txtBatchNumber != null && productDetail.getBatchNumber() != null) {
                        txtBatchNumber.setText(productDetail.getBatchNumber());
                    }
                    if (dateManufacturing != null && productDetail.getManufacturingDate() != null) {
                        dateManufacturing.setValue(productDetail.getManufacturingDate());
                    }
                    if (dateExpiry != null && productDetail.getExpiryDate() != null) {
                        dateExpiry.setValue(productDetail.getExpiryDate());
                    }
                    // Load supplier if supplier name exists
                    if (cmbSupplier != null && productDetail.getSupplierName() != null) {
                        // Find supplier by name
                        Supplier supplier = supplierService.findAllSuppliers().stream()
                            .filter(s -> s.getName().equals(productDetail.getSupplierName()))
                            .findFirst()
                            .orElse(null);
                        if (supplier != null) {
                            @SuppressWarnings("unchecked")
                            ComboBox<Supplier> supplierCombo = (ComboBox<Supplier>) cmbSupplier;
                            supplierCombo.setValue(supplier);
                        }
                    }
                    if (txtSupplierContact != null && productDetail.getSupplierContact() != null) {
                        txtSupplierContact.setText(productDetail.getSupplierContact());
                    }
                    
                    uniqueData = productDetail.getCode();
                    
                    // Show existing batch code in manual barcode field (read-only in edit mode)
                    if (txtBatchBarcodeCode != null && productDetail.getCode() != null) {
                        txtBatchBarcodeCode.setText(productDetail.getCode());
                        txtBatchBarcodeCode.setEditable(false); // Make read-only in edit mode
                    }

                    // Load barcode image
                    if (barcodeImage != null && productDetail.getBarcode() != null && !productDetail.getBarcode().isEmpty()) {
                        try {
                    byte[] data = Base64.getDecoder().decode(productDetail.getBarcode());
                            barcodeImage.setImage(new Image(new ByteArrayInputStream(data)));
                        } catch (Exception e) {
                            System.err.println("Error loading barcode image: " + e.getMessage());
                        }
                    }
                } else {
                    new Alert(Alert.AlertType.ERROR, "Batch not found!").show();
                    if (stage != null) stage.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error loading batch data: " + e.getMessage()).show();
                if (stage != null) stage.close();
            }
        } else {
            // NEW BATCH MODE
            try {
                // Clear and enable manual barcode field for new batch
                if (txtBatchBarcodeCode != null) {
                    txtBatchBarcodeCode.clear();
                    txtBatchBarcodeCode.setEditable(true); // Make editable for new batch
                }
                
                // Generate auto barcode (will be used if user doesn't enter manual code)
                generateBatchBarcode();
                
                // Auto-generate batch number
                if (txtBatchNumber != null) {
                    txtBatchNumber.setText(generateBatchNumber());
                }
                
                // Set default low stock threshold
                if (txtLowStockThreshold != null) {
                    txtLowStockThreshold.setText("10");
                }

                // Pre-fill quantity with 1
                if (txtQty != null) {
                    txtQty.setText("1");
                    txtQty.requestFocus();
                }
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error initializing batch form: " + e.getMessage()).show();
            }
        }
    }

    /**
     * Validate all inputs before saving
     */
    private boolean validateInputs() {
        // Required field validation
        if (txtQty == null) {
            showError("Quantity field failed to load. Please close and reopen the batch form.");
            return false;
        }
        if (txtQty.getText() == null || txtQty.getText().trim().isEmpty()) {
            highlightField(txtQty);
            showError("Quantity is required!");
            txtQty.requestFocus();
            return false;
        }
        clearFieldHighlight(txtQty);
        
        if (txtBuyingPrice == null || txtBuyingPrice.getText() == null || txtBuyingPrice.getText().trim().isEmpty()) {
            if (txtBuyingPrice != null) highlightField(txtBuyingPrice);
            showError("Buying price is required!");
            return false;
        }
        clearFieldHighlight(txtBuyingPrice);
        
        if (txtSellingPrice == null || txtSellingPrice.getText() == null || txtSellingPrice.getText().trim().isEmpty()) {
            if (txtSellingPrice != null) highlightField(txtSellingPrice);
            showError("Selling price is required!");
            return false;
        }
        clearFieldHighlight(txtSellingPrice);
        
        try {
            // Validate quantity
            int qty = Integer.parseInt(txtQty.getText().trim());
            if (qty <= 0) {
                showError("Quantity must be greater than 0!");
                return false;
            }
            if (qty > 1000000) {
                showError("Quantity seems unrealistic. Please verify!");
                return false;
            }
            
            // Validate low stock threshold
            if (txtLowStockThreshold != null && !txtLowStockThreshold.getText().trim().isEmpty()) {
                int threshold = Integer.parseInt(txtLowStockThreshold.getText().trim());
                if (threshold < 0) {
                    showError("Low stock threshold cannot be negative!");
                    return false;
                }
                if (threshold >= qty) {
                    showWarning("Low stock threshold should be less than quantity. Continue anyway?");
                }
            }
            
            // Validate prices
            double buyingPrice = Double.parseDouble(txtBuyingPrice.getText().trim());
            double sellingPrice = Double.parseDouble(txtSellingPrice.getText().trim());
            
            if (buyingPrice < 0 || sellingPrice < 0) {
                showError("Prices cannot be negative!");
                return false;
            }
            
            if (sellingPrice < buyingPrice) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Price Warning");
                confirmAlert.setHeaderText("Selling price is less than buying price!");
                confirmAlert.setContentText("This batch will result in a loss. Do you want to continue?");
                var result = confirmAlert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return false;
                }
            }
            
            // Validate profit margin
            double profitMargin = ((sellingPrice - buyingPrice) / buyingPrice) * 100;
            if (profitMargin < 5) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Low Profit Warning");
                confirmAlert.setHeaderText("Profit margin is very low (" + String.format("%.2f%%", profitMargin) + ")");
                confirmAlert.setContentText("Consider increasing the selling price. Continue anyway?");
                var result = confirmAlert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return false;
                }
            }
            
            // Validate dates
            if (dateManufacturing != null && dateManufacturing.getValue() != null) {
                if (dateManufacturing.getValue().isAfter(LocalDate.now())) {
                    showError("Manufacturing date cannot be in the future!");
                    return false;
                }
            }
            
            if (dateExpiry != null && dateExpiry.getValue() != null) {
                if (dateExpiry.getValue().isBefore(LocalDate.now())) {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Expired Product");
                    confirmAlert.setHeaderText("Expiry date is in the past!");
                    confirmAlert.setContentText("This batch is already expired. Continue anyway?");
                    var result = confirmAlert.showAndWait();
                    if (result.isEmpty() || result.get() != ButtonType.OK) {
                        return false;
                    }
                }
                
                // Check if expiry is within 30 days
                long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dateExpiry.getValue());
                if (daysUntilExpiry > 0 && daysUntilExpiry <= 30) {
                    showWarning("Warning: This batch will expire in " + daysUntilExpiry + " days!");
                }
            }
            
            // Validate manufacturing and expiry date relationship
            if (dateManufacturing != null && dateManufacturing.getValue() != null &&
                dateExpiry != null && dateExpiry.getValue() != null) {
                if (!dateExpiry.getValue().isAfter(dateManufacturing.getValue())) {
                    showError("Expiry date must be after manufacturing date!");
                    return false;
                }
            }
            
        } catch (NumberFormatException e) {
            showError("Invalid number format. Please check your inputs!");
            return false;
        }
        
        return true;
    }

    /**
     * Save batch with comprehensive validation
     */
    @FXML
    @Transactional
    public void saveBatch(ActionEvent actionEvent) {
        try {
            // Validate all inputs
            if (!validateInputs()) {
                return;
            }
            
            // Create or update product detail
            ProductDetail productDetail = new ProductDetail();
            
            // Set batch code - use manual code if provided, otherwise use auto-generated
            String batchCodeToUse = null;
            if (isEditMode && existingBatchCode != null) {
                batchCodeToUse = existingBatchCode;
            } else {
                // Check if user entered manual barcode code
                if (txtBatchBarcodeCode != null && txtBatchBarcodeCode.getText() != null && 
                    !txtBatchBarcodeCode.getText().trim().isEmpty()) {
                    batchCodeToUse = txtBatchBarcodeCode.getText().trim();
                    uniqueData = batchCodeToUse; // Update uniqueData to match manual code
                } else {
                    // Use auto-generated code
                    batchCodeToUse = uniqueData;
                }
            }
            
            if (batchCodeToUse == null || batchCodeToUse.trim().isEmpty()) {
                showError("Batch code is required! Please enter a manual code or ensure auto-generation works.");
                return;
            }
            
            productDetail.setCode(batchCodeToUse);
            
            // Generate and set barcode image
            if (!isEditMode) {
                try {
                    // Generate barcode for display using the batch code
                    Code128Writer barcodeWriter = new Code128Writer();
                    BitMatrix bitMatrix = barcodeWriter.encode(
                        batchCodeToUse,
                        BarcodeFormat.CODE_128,
                        300,
                        80
                    );
                    
                    BufferedImage barcodeBufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
                    
                    // Convert to Base64 for storage
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(barcodeBufferedImage, "png", baos);
                    byte[] barcodeBytes = baos.toByteArray();
                    productDetail.setBarcode(Base64.getEncoder().encodeToString(barcodeBytes));
                } catch (Exception e) {
                    System.err.println("Error generating barcode: " + e.getMessage());
                    productDetail.setBarcode("");
                }
            } else {
                // Keep existing barcode for edit mode
                ProductDetail existing = productDetailService.findProductDetail(existingBatchCode);
                if (existing != null && existing.getBarcode() != null) {
                    productDetail.setBarcode(existing.getBarcode());
                }
            }
            
            // Set quantity fields
            int qty = Integer.parseInt(txtQty.getText().trim());
            productDetail.setQtyOnHand(qty);
            
            if (isEditMode) {
                // Keep initial qty for edit mode
                ProductDetail existing = productDetailService.findProductDetail(existingBatchCode);
                if (existing != null) {
                    productDetail.setInitialQty(existing.getInitialQty());
                }
            } else {
                // Set initial qty for new batch
                productDetail.setInitialQty(qty);
            }
            
            if (txtLowStockThreshold != null && !txtLowStockThreshold.getText().trim().isEmpty()) {
                productDetail.setLowStockThreshold(Integer.parseInt(txtLowStockThreshold.getText().trim()));
            }
            
            // Set pricing
            productDetail.setBuyingPrice(Double.parseDouble(txtBuyingPrice.getText().trim()));
            productDetail.setSellingPrice(Double.parseDouble(txtSellingPrice.getText().trim()));
            // Set show price same as selling price
            productDetail.setShowPrice(Double.parseDouble(txtSellingPrice.getText().trim()));
            
            // Set discount (always false since discount section is removed)
            productDetail.setDiscountAvailability(false);
            productDetail.setDiscountRate(0.0);
            
            // Set batch tracking info
            if (txtBatchNumber != null && !txtBatchNumber.getText().trim().isEmpty()) {
                productDetail.setBatchNumber(txtBatchNumber.getText().trim());
            }
            
            if (dateManufacturing != null && dateManufacturing.getValue() != null) {
                productDetail.setManufacturingDate(dateManufacturing.getValue());
            }
            
            if (dateExpiry != null && dateExpiry.getValue() != null) {
                productDetail.setExpiryDate(dateExpiry.getValue());
            }
            
            // Set supplier info
            if (cmbSupplier != null && cmbSupplier.getValue() != null) {
                @SuppressWarnings("unchecked")
                Supplier selectedSupplier = (Supplier) cmbSupplier.getValue();
                productDetail.setSupplierName(selectedSupplier.getName());
                
                // Set supplier contact from the auto-filled field
                if (txtSupplierContact != null && !txtSupplierContact.getText().trim().isEmpty()) {
                    productDetail.setSupplierContact(txtSupplierContact.getText().trim());
                } else if (selectedSupplier.getPhone() != null) {
                    productDetail.setSupplierContact(selectedSupplier.getPhone());
                } else if (selectedSupplier.getEmail() != null) {
                    productDetail.setSupplierContact(selectedSupplier.getEmail());
                }
            }
            
            // Set product code
            if (currentProductCode != null) {
                productDetail.setProductCode(currentProductCode);
            } else if (txtProductCode != null && !txtProductCode.getText().trim().isEmpty()) {
                productDetail.setProductCode(Integer.parseInt(txtProductCode.getText().trim()));
            } else {
                showError("Product code is required!");
                return;
            }
            
            // Save to database
            productDetailService.saveProductDetail(productDetail);
            
            // Show success message
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Success");
            successAlert.setHeaderText(null);
            successAlert.setContentText(isEditMode ? "Batch updated successfully!" : "Batch created successfully!");
            successAlert.showAndWait();
            
            // Close the form
            if (this.stage != null) {
                this.stage.close();
            }
            
        } catch (NumberFormatException e) {
            showError("Invalid number format. Please check your inputs!");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error saving batch: " + e.getMessage());
        }
    }

    /**
     * Show error alert
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show warning alert
     */
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void highlightField(TextInputControl field) {
        if (field != null) {
            field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        }
    }

    private void clearFieldHighlight(TextInputControl field) {
        if (field != null) {
            field.setStyle("");
        }
    }

    /**
     * Reset editable fields to default state when opening a new batch form
     */
    private void resetFormFields() {
        if (txtQty != null) {
            txtQty.setText("1");
            clearFieldHighlight(txtQty);
        }
        if (txtLowStockThreshold != null) {
            txtLowStockThreshold.clear();
            clearFieldHighlight(txtLowStockThreshold);
        }
        if (txtBuyingPrice != null) {
            txtBuyingPrice.clear();
            clearFieldHighlight(txtBuyingPrice);
        }
        if (txtSellingPrice != null) {
            txtSellingPrice.clear();
            clearFieldHighlight(txtSellingPrice);
        }
        if (txtProfitMargin != null) {
            txtProfitMargin.clear();
            clearFieldHighlight(txtProfitMargin);
        }
        if (txtBatchNumber != null) {
            // Batch number is auto-generated, don't clear it
            clearFieldHighlight(txtBatchNumber);
        }
        if (dateManufacturing != null) {
            dateManufacturing.setValue(null);
        }
        if (dateExpiry != null) {
            dateExpiry.setValue(null);
        }
        if (cmbSupplier != null) {
            cmbSupplier.setValue(null);
        }
        if (txtSupplierContact != null) {
            txtSupplierContact.clear();
        }
        if (txtBatchBarcodeCode != null) {
            txtBatchBarcodeCode.clear();
        }
    }
}
