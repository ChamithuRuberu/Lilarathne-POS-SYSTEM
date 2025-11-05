package com.devstack.pos.controller;

import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.ProductService;
import com.devstack.pos.util.BarcodeGenerator;
import com.devstack.pos.view.tm.ProductDetailTm;
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
    public TextField txtShowPrice;
    @FXML
    public TextField txtProfitMargin;
    
    // Discount Fields
    @FXML
    public RadioButton rBtnYes;
    @FXML
    public TextField txtDiscountRate;
    
    // Batch Tracking Fields
    @FXML
    public TextField txtBatchNumber;
    @FXML
    public DatePicker dateManufacturing;
    @FXML
    public DatePicker dateExpiry;
    
    // Supplier Fields
    @FXML
    public TextField txtSupplierName;
    @FXML
    public TextField txtSupplierContact;
    
    // Notes
    @FXML
    public TextArea txtNotes;
    
    String uniqueData = null;
    Stage stage = null;
    private Integer currentProductCode = null;
    private boolean isEditMode = false;
    private String existingBatchCode = null;

    private final ProductDetailService productDetailService;
    private final ProductService productService;

    @FXML
    public void initialize() {
        setupProfitMarginCalculation();
        setupDateValidation();
        setupNumericValidation();
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
        setupDecimalField(txtShowPrice);
        setupDecimalField(txtDiscountRate);
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
     * Generate unique batch barcode
     */
    private void generateBatchBarcode() throws WriterException, IOException {
        // Generate unique numeric barcode for batch
        uniqueData = BarcodeGenerator.generateNumeric(12);
        
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
                    if (txtShowPrice != null) txtShowPrice.setText(String.format("%.2f", productDetail.getShowPrice()));
                    if (rBtnYes != null) rBtnYes.setSelected(productDetail.isDiscountAvailability());
                    if (txtDiscountRate != null && productDetail.getDiscountRate() > 0) {
                        txtDiscountRate.setText(String.format("%.2f", productDetail.getDiscountRate()));
                    }
                    if (txtBatchNumber != null && productDetail.getBatchNumber() != null) {
                        txtBatchNumber.setText(productDetail.getBatchNumber());
                    }
                    if (dateManufacturing != null && productDetail.getManufacturingDate() != null) {
                        dateManufacturing.setValue(productDetail.getManufacturingDate());
                    }
                    if (dateExpiry != null && productDetail.getExpiryDate() != null) {
                        dateExpiry.setValue(productDetail.getExpiryDate());
                    }
                    if (txtSupplierName != null && productDetail.getSupplierName() != null) {
                        txtSupplierName.setText(productDetail.getSupplierName());
                    }
                    if (txtSupplierContact != null && productDetail.getSupplierContact() != null) {
                        txtSupplierContact.setText(productDetail.getSupplierContact());
                    }
                    if (txtNotes != null && productDetail.getNotes() != null) {
                        txtNotes.setText(productDetail.getNotes());
                    }
                    
                    uniqueData = productDetail.getCode();

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
        
        if (txtShowPrice == null || txtShowPrice.getText() == null || txtShowPrice.getText().trim().isEmpty()) {
            if (txtShowPrice != null) highlightField(txtShowPrice);
            showError("Show price is required!");
            return false;
        }
        clearFieldHighlight(txtShowPrice);
        
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
            double showPrice = Double.parseDouble(txtShowPrice.getText().trim());
            
            if (buyingPrice < 0 || sellingPrice < 0 || showPrice < 0) {
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
            
            // Validate discount rate
            if (rBtnYes != null && rBtnYes.isSelected()) {
                if (txtDiscountRate == null || txtDiscountRate.getText().trim().isEmpty()) {
                    showError("Please enter discount rate when discount is available!");
                    return false;
                }
                double discountRate = Double.parseDouble(txtDiscountRate.getText().trim());
                if (discountRate < 0 || discountRate > 100) {
                    showError("Discount rate must be between 0 and 100!");
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
            
            // Set batch code
            if (isEditMode && existingBatchCode != null) {
                productDetail.setCode(existingBatchCode);
            } else {
            productDetail.setCode(uniqueData);
            }
            
            // Generate and set barcode image
            if (!isEditMode) {
                try {
                    // Generate barcode for display
                    Code128Writer barcodeWriter = new Code128Writer();
                    BitMatrix bitMatrix = barcodeWriter.encode(
                        uniqueData,
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
            productDetail.setShowPrice(Double.parseDouble(txtShowPrice.getText().trim()));
            
            // Set discount
            productDetail.setDiscountAvailability(rBtnYes != null && rBtnYes.isSelected());
            if (txtDiscountRate != null && !txtDiscountRate.getText().trim().isEmpty()) {
                productDetail.setDiscountRate(Double.parseDouble(txtDiscountRate.getText().trim()));
            }
            
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
            if (txtSupplierName != null && !txtSupplierName.getText().trim().isEmpty()) {
                productDetail.setSupplierName(txtSupplierName.getText().trim());
            }
            
            if (txtSupplierContact != null && !txtSupplierContact.getText().trim().isEmpty()) {
                productDetail.setSupplierContact(txtSupplierContact.getText().trim());
            }
            
            // Set notes
            if (txtNotes != null && !txtNotes.getText().trim().isEmpty()) {
                productDetail.setNotes(txtNotes.getText().trim());
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
        if (txtShowPrice != null) {
            txtShowPrice.clear();
            clearFieldHighlight(txtShowPrice);
        }
        if (txtProfitMargin != null) {
            txtProfitMargin.clear();
            clearFieldHighlight(txtProfitMargin);
        }
        if (txtDiscountRate != null) {
            txtDiscountRate.clear();
            clearFieldHighlight(txtDiscountRate);
        }
        if (txtBatchNumber != null) {
            txtBatchNumber.clear();
            clearFieldHighlight(txtBatchNumber);
        }
        if (dateManufacturing != null) {
            dateManufacturing.setValue(null);
        }
        if (dateExpiry != null) {
            dateExpiry.setValue(null);
        }
        if (txtSupplierName != null) {
            txtSupplierName.clear();
        }
        if (txtSupplierContact != null) {
            txtSupplierContact.clear();
        }
        if (txtNotes != null) {
            txtNotes.clear();
            clearFieldHighlight(txtNotes);
        }
        if (rBtnYes != null) {
            rBtnYes.setSelected(false);
        }
    }
}
