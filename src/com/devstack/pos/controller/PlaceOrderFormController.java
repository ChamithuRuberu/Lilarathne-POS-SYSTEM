package com.devstack.pos.controller;

import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.OrderItem;
import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.OrderItemService;
import com.devstack.pos.service.PDFReportService;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.ProductService;
import com.devstack.pos.util.AuthorizationUtil;
import com.devstack.pos.util.UserSessionData;
import com.devstack.pos.view.tm.CartTm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PlaceOrderFormController extends BaseController {
    public TextField txtContact;
    public TextField txtName;
    public TextField txtBarcode;
    public TextField txtDescription;
    public TextField txtSellingPrice;
    public TextField txtDiscount;
    public TextField txtQtyOnHand;
    public TextField txtBuyingPrice;
    public TextField txtQty;
    public TableView<CartTm> tblCart;
    public TableColumn colCode;
    public TableColumn colDesc;
    public TableColumn colSelPrice;
    public TableColumn colSelDisc;
    public TableColumn colSelQty;
    public TableColumn colSelTotal;
    public TableColumn colSelOperation;
    public Text txtTotal;
    public Text txtBalance;
    public JFXTextField txtCustomerPaid;
    public JFXButton btnPrint;
    public JFXComboBox<String> cmbPaymentMethod;

    private Long selectedCustomerId = null;
    private Long lastCompletedOrderCode = null;
    private final ProductDetailService productDetailService;
    private final ProductService productService;
    private final OrderDetailService orderDetailService;
    private final OrderItemService orderItemService;
    private final PDFReportService pdfReportService;
    private boolean isUpdatingBarcodeProgrammatically = false;


    public void initialize() {
        // Initialize sidebar
        initializeSidebar();
        
        // Authorization check: POS Orders accessible by ADMIN and CASHIER
        if (!AuthorizationUtil.canAccessPOSOrders()) {
            AuthorizationUtil.showUnauthorizedAlert();
            btnDashboardOnAction(null);
            return;
        }
        
        // Set table column resize policy
        tblCart.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Initialize table columns
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colSelPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        colSelDisc.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colSelQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colSelTotal.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        colSelOperation.setCellValueFactory(new PropertyValueFactory<>("btn"));
        
        // Format quantity column to show decimals properly
        colSelQty.setCellFactory(column -> new TableCell<CartTm, Double>() {
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
        
        // Format total cost column to show currency properly
        colSelTotal.setCellFactory(column -> new TableCell<CartTm, Double>() {
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
        
        // Initialize payment method dropdown
        if (cmbPaymentMethod != null) {
            cmbPaymentMethod.setItems(FXCollections.observableArrayList("Cash", "Credit", "Cheque"));
            cmbPaymentMethod.setValue("Cash"); // Set default to Cash
        }
        
        // Initialize balance display
        if (txtBalance != null) {
            txtBalance.setText("0.00 /=");
            txtBalance.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: #dc2626;");
        }
        
        // Disable print button initially (no order to print yet)
        if (btnPrint != null) {
            btnPrint.setDisable(true);
        }
        
        // Setup decimal quantity input validation (supports items like sand by kg, pipes by meters)
        setupQuantityInputValidation();
        
        // Setup barcode scanner detection for automatic product loading
        setupBarcodeScannerDetection();
    }
    
    /**
     * Setup validation for quantity input field to support decimal quantities
     * This allows items like sand (sold by kg) and metal pipes (sold by meters) to use decimal quantities
     */
    private void setupQuantityInputValidation() {
        if (txtQty != null) {
            txtQty.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null || newValue.isEmpty()) {
                    return; // Allow empty for user to type
                }
                // Allow decimal numbers (e.g., 2.5, 3.75, 10.5)
                // Pattern: optional negative sign, digits, optional decimal point with digits
                if (!newValue.matches("^\\d*\\.?\\d*$")) {
                    txtQty.setText(oldValue);
                }
            });
        }
    }
    
    
    @Override
    protected String getCurrentPageName() {
        return "Place Order";
    }

    public void btnBackToHomeOnAction(ActionEvent actionEvent) {
        btnDashboardOnAction(actionEvent);
    }

    public void btnAddNewCustomerOnAction(ActionEvent actionEvent) {
        // Customer management feature moved to Feature/customer-module branch
        new Alert(Alert.AlertType.INFORMATION, "Customer management feature has been moved to Feature/customer-module branch").show();
    }

    public void btnAddNewProductOnAction(ActionEvent actionEvent) throws IOException {
        setUi("ProductMainForm", true);
    }

    private void setUi(String url, boolean state) throws IOException {
        Stage stage = null;
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/com/devstack/pos/view/" + url + ".fxml"));
        loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
        Scene scene = new Scene(loader.load());

        if (state) {
            stage = new Stage();
            stage.setScene(scene);
            stage.show();
        } else {
            stage = (Stage) context.getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
        }
    }

    public void searchCustomer(ActionEvent actionEvent) {
        try {
            String contact = txtContact.getText().trim();
            if (contact.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please enter a contact number!").show();
                return;
            }
            
            // Customer management feature moved to Feature/customer-module branch
            selectedCustomerId = null;
            txtName.setText("Guest");
            new Alert(Alert.AlertType.INFORMATION, "Proceeding with guest checkout. Customer management feature has been moved to Feature/customer-module branch.").show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).show();
            e.printStackTrace();
        }
    }

    /**
     * Sets up barcode scanner detection - only triggers on Enter key press
     * This prevents auto-filling while user is typing
     * User must press Enter to load the product
     */
    private void setupBarcodeScannerDetection() {
        // Remove automatic debounce - only trigger on Enter key (handled by onAction in FXML)
        // This allows users to type freely without interruption
        
        // Listen for Enter key or newline (from barcode scanners) to trigger product loading
        txtBarcode.setOnKeyPressed(event -> {
            // Check for Enter key or newline character (barcode scanners often send newline)
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                event.consume(); // Prevent default behavior
                loadProduct(null);
            }
        });
        
        // Also handle text changes for barcode scanners that append newline
        txtBarcode.textProperty().addListener((observable, oldValue, newValue) -> {
            // Don't process if we're updating programmatically
            if (isUpdatingBarcodeProgrammatically) {
                return;
            }
            
            // Check if barcode scanner appended a newline (common behavior)
            if (newValue != null && (newValue.endsWith("\n") || newValue.endsWith("\r"))) {
                // Remove newline and trigger product loading
                String cleanedBarcode = newValue.replace("\n", "").replace("\r", "").trim();
                if (!cleanedBarcode.isEmpty()) {
                    // Set the cleaned barcode
                    isUpdatingBarcodeProgrammatically = true;
                    try {
                        txtBarcode.setText(cleanedBarcode);
                    } finally {
                        javafx.application.Platform.runLater(() -> {
                            isUpdatingBarcodeProgrammatically = false;
                        });
                    }
                    // Trigger product loading
                    javafx.application.Platform.runLater(() -> {
                        loadProduct(null);
                    });
                }
            }
        });
        
        // Auto-focus barcode field when form loads
        javafx.application.Platform.runLater(() -> {
            txtBarcode.requestFocus();
        });
    }

    public void loadProduct(ActionEvent actionEvent) {
        try {
            String input = txtBarcode.getText() == null ? "" : txtBarcode.getText().trim();
            if (input.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please enter a barcode or product code!").show();
                return;
            }
            
            // Try by batch code or barcode first
            ProductDetail productDetail = productDetailService.findByCodeWithProduct(input);
            
            // If not found, try by product code (numeric)
            if (productDetail == null) {
                try {
                    int productCode = Integer.parseInt(input);
                    List<ProductDetail> activeBatches = productDetailService.findActiveBatchesByProductCode(productCode);
                    if (!activeBatches.isEmpty()) {
                        productDetail = activeBatches.get(0);
                    }
                } catch (NumberFormatException ignored) {
                    // input is not a product code
                }
            }
            
            if (productDetail != null) {
                // Load product description using product code
                Product product = productService.findProduct(productDetail.getProductCode());
                if (product != null) {
                    txtDescription.setText(product.getDescription());
                }
                // Update barcode field with the batch code (only if not currently being edited)
                // Set flag to prevent listener from triggering during programmatic update
                isUpdatingBarcodeProgrammatically = true;
                try {
                    // Only update if the current text doesn't match (to avoid interrupting typing)
                    String currentText = txtBarcode.getText();
                    if (currentText == null || !currentText.trim().equals(productDetail.getCode())) {
                        txtBarcode.setText(productDetail.getCode());
                    }
                } finally {
                    javafx.application.Platform.runLater(() -> {
                        isUpdatingBarcodeProgrammatically = false;
                    });
                }
                txtSellingPrice.setText(String.format("%.2f", productDetail.getSellingPrice()));
                // Format quantity - show as integer if whole number, otherwise show decimals
                double qtyOnHand = productDetail.getQtyOnHand();
                if (qtyOnHand == (int)qtyOnHand) {
                    txtQtyOnHand.setText(String.valueOf((int)qtyOnHand));
                } else {
                    txtQtyOnHand.setText(String.format("%.2f", qtyOnHand));
                }
                txtBuyingPrice.setText(String.format("%.2f", productDetail.getBuyingPrice()));
                
                // Show stock status
                if (productDetail.getQtyOnHand() <= 0) {
                    txtQtyOnHand.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                } else if (productDetail.isLowStock()) {
                    txtQtyOnHand.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                } else {
                    txtQtyOnHand.setStyle("-fx-text-fill: #16a34a;");
                }
                
                // Show success message
                String productName = product != null ? product.getDescription() : "Product";
//                new Alert(Alert.AlertType.INFORMATION,
//                    "Product loaded successfully!\n\n" +
//                    "Product: " + productName + "\n" +
//                    "Batch Code: " + productDetail.getCode() + "\n" +
//                    "Stock: " + (qtyOnHand == (int)qtyOnHand ? String.valueOf((int)qtyOnHand) : String.format("%.2f", qtyOnHand)) + " units"
//                ).show();
                
                txtQty.requestFocus();
            } else {
                new Alert(Alert.AlertType.WARNING, 
                    "Product not found!\n\n" +
                    "Please check:\n" +
                    "• Barcode/Code is correct\n" +
                    "• Product exists in the system\n" +
                    "• Product has active batches\n\n" +
                    "You can add a new product using the '+ New Product' button.").show();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, 
                "Error loading product: " + e.getMessage() + 
                "\n\nPlease try again or contact support if the issue persists.").show();
            e.printStackTrace();
        }
    }

    ObservableList<CartTm> tms = FXCollections.observableArrayList();

    /**
     * Adds product to cart with proper validation for quantity and stock availability
     * Supports decimal quantities for items like sand (kg) and metal pipes (meters)
     */
    public void addToCart(ActionEvent actionEvent) {
        try {
            // Validate required fields
            if (txtBarcode.getText() == null || txtBarcode.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please load a product first by entering barcode/code!").show();
                txtBarcode.requestFocus();
                return;
            }
            
            if (txtDescription.getText() == null || txtDescription.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Product description is missing! Please load the product first.").show();
                txtBarcode.requestFocus();
                return;
            }
            
            // Validate and parse quantity (supports decimal for items like sand, pipes)
            String qtyText = txtQty.getText() == null ? "" : txtQty.getText().trim();
            if (qtyText.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please enter quantity!").show();
                txtQty.requestFocus();
                return;
            }
            
            double qty;
            try {
                qty = Double.parseDouble(qtyText);
                if (qty <= 0) {
                    new Alert(Alert.AlertType.WARNING, "Quantity must be greater than zero!").show();
                    txtQty.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.WARNING, "Please enter a valid quantity (e.g., 2 or 2.5)!").show();
                txtQty.requestFocus();
                return;
            }
            
            // Validate selling price
            String priceText = txtSellingPrice.getText() == null ? "" : txtSellingPrice.getText().trim();
            if (priceText.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Selling price is missing! Please load the product first.").show();
                return;
            }
            
            double unitPrice;
            try {
                unitPrice = Double.parseDouble(priceText);
                if (unitPrice < 0) {
                    new Alert(Alert.AlertType.WARNING, "Selling price cannot be negative!").show();
                    return;
                }
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.WARNING, "Invalid selling price! Please load the product again.").show();
                return;
            }
            
            // Check stock availability (only if product detail exists)
            String batchCode = txtBarcode.getText().trim();
            ProductDetail productDetail = productDetailService.findProductDetailByCode(batchCode);
            if (productDetail != null) {
                // Get available quantity (supports decimal quantities)
                double availableQty = productDetail.getQtyOnHand();
                
                // Check if requested quantity exceeds available stock
                if (qty > availableQty) {
                    // Format available quantity for display
                    String availableStr;
                    if (availableQty == (int)availableQty) {
                        availableStr = String.valueOf((int)availableQty);
                    } else {
                        availableStr = String.format("%.2f", availableQty);
                    }
                    
                    // Format requested quantity for display
                    String requestedStr;
                    if (qty == (int)qty) {
                        requestedStr = String.valueOf((int)qty);
                    } else {
                        requestedStr = String.format("%.2f", qty);
                    }
                    
                    new Alert(Alert.AlertType.WARNING, 
                        String.format("Insufficient stock!\n\n" +
                            "Available: %s\n" +
                            "Requested: %s\n" +
                            "Please reduce the quantity or check other batches.", 
                            availableStr, requestedStr)).show();
                    txtQty.requestFocus();
                    return;
                }
            }
            
            // Parse discount
            double unitDiscount = 0.0;
            String discountText = txtDiscount.getText() == null ? "" : txtDiscount.getText().trim();
            if (!discountText.isEmpty()) {
                try {
                    unitDiscount = Double.parseDouble(discountText);
                    if (unitDiscount < 0) {
                        new Alert(Alert.AlertType.WARNING, "Discount cannot be negative!").show();
                        txtDiscount.requestFocus();
                        return;
                    }
                    if (unitDiscount > unitPrice) {
                        new Alert(Alert.AlertType.WARNING, 
                            String.format("Discount (%.2f) cannot exceed selling price (%.2f)!", 
                                unitDiscount, unitPrice)).show();
                        txtDiscount.requestFocus();
                        return;
                    }
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.WARNING, "Invalid discount amount! Please enter a valid number.").show();
                    txtDiscount.requestFocus();
                    return;
                }
            }
            
            // Calculate costs
            double effectiveUnitPrice = unitPrice - unitDiscount;
            if (effectiveUnitPrice < 0) effectiveUnitPrice = 0.0;
            double totalCost = qty * effectiveUnitPrice;

            // Disable print button when starting a new order (first item added to empty cart)
            if (btnPrint != null && tms.isEmpty()) {
                btnPrint.setDisable(true);
                lastCompletedOrderCode = null;
            }

            // Check if product already exists in cart
            CartTm selectedCartTm = isExists(batchCode);
            if (selectedCartTm != null) {
                // Update existing cart item
                double newQty = selectedCartTm.getQty() + qty;
                double newTotalCost = selectedCartTm.getTotalCost() + totalCost;
                
                // Re-validate stock for updated quantity (supports decimal quantities)
                if (productDetail != null) {
                    double availableQty = productDetail.getQtyOnHand();
                    if (newQty > availableQty) {
                        // Format quantities for display
                        String availableStr = availableQty == (int)availableQty ? 
                            String.valueOf((int)availableQty) : String.format("%.2f", availableQty);
                        String currentStr = selectedCartTm.getQty() == (int)selectedCartTm.getQty() ? 
                            String.valueOf((int)selectedCartTm.getQty()) : String.format("%.2f", selectedCartTm.getQty());
                        String addingStr = qty == (int)qty ? 
                            String.valueOf((int)qty) : String.format("%.2f", qty);
                        String totalStr = newQty == (int)newQty ? 
                            String.valueOf((int)newQty) : String.format("%.2f", newQty);
                        
                        new Alert(Alert.AlertType.WARNING, 
                            String.format("Cannot add more quantity!\n\n" +
                                "Current in cart: %s\n" +
                                "Adding: %s\n" +
                                "Total would be: %s\n" +
                                "But available stock is only: %s", 
                                currentStr, addingStr, totalStr, availableStr)).show();
                        return;
                    }
                }
                
                selectedCartTm.setQty(newQty);
                selectedCartTm.setTotalCost(newTotalCost);
                tblCart.refresh();
                setTotal();
            } else {
                // Add new item to cart
                Button btn = new Button("Remove");
                CartTm tm = new CartTm(batchCode,
                        txtDescription.getText(),
                        unitDiscount,
                        effectiveUnitPrice,
                        0.0,
                        qty,
                        totalCost,
                        btn);

                btn.setOnAction((e) -> {
                    tms.remove(tm);
                    tblCart.refresh();
                    setTotal();
                });

                tms.add(tm);
                clear();
                tblCart.setItems(tms);
                setTotal();
            }
            
//            // Show success message for first item
//            if (tms.size() == 1) {
//                javafx.application.Platform.runLater(() -> {
//                    new Alert(Alert.AlertType.INFORMATION,
//                        "Product added to cart!\n\n" +
//                        "You can continue adding more products or complete the order.").show();
//                });
//            }
            
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, 
                "Error adding product to cart: " + e.getMessage() + 
                "\n\nPlease check all fields and try again.").show();
        }
    }

    private void clear() {
        txtDescription.clear();
        txtSellingPrice.clear();
        txtDiscount.clear();
        txtQtyOnHand.clear();
        txtBuyingPrice.clear();
        txtQty.clear();
        txtBarcode.clear();
        txtBarcode.requestFocus();
    }

    private CartTm isExists(String code) {
        for (CartTm tm : tms
        ) {
            if (tm.getCode().equals(code)) {
                return tm;
            }
        }
        return null;
    }

    private void setTotal() {
        double total = 0;
        for (CartTm tm : tms) {
            total += tm.getTotalCost();
        }
        txtTotal.setText(String.format("%.2f /=", total));
        calculateBalance();
    }
    
    public void calculateBalance(javafx.scene.input.KeyEvent keyEvent) {
        calculateBalance();
    }
    
    private void calculateBalance() {
        try {
            double total = Double.parseDouble(txtTotal.getText().split(" /=")[0]);
            double customerPaid = 0.0;
            String paidText = txtCustomerPaid.getText() == null ? "" : txtCustomerPaid.getText().trim();
            if (!paidText.isEmpty()) {
                customerPaid = Double.parseDouble(paidText);
            }
            double balance = customerPaid - total;
            txtBalance.setText(String.format("%.2f /=", balance));
            
            // Change color based on balance (red for negative, green for positive)
            if (balance < 0) {
                txtBalance.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: #dc2626;");
            } else if (balance > 0) {
                txtBalance.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: #16a34a;");
            } else {
                txtBalance.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: #64748b;");
            }
        } catch (Exception e) {
            txtBalance.setText("0.00 /=");
            txtBalance.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: #dc2626;");
        }
    }

    public void btnCompleteOrder(ActionEvent actionEvent) {
        try {
            if (tms.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Cart is empty!").show();
                return;
            }
            
            // Validate customer paid field - must not be null or empty
            if (txtCustomerPaid == null) {
                new Alert(Alert.AlertType.ERROR, "Customer paid field is not initialized!").show();
                return;
            }
            
            String paidText = txtCustomerPaid.getText();
            if (paidText == null || paidText.trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please enter the amount paid by the customer!").show();
                txtCustomerPaid.requestFocus();
                return;
            }
            
            // Calculate total discount (unit discount * qty)
            double totalDiscount = tms.stream()
                    .mapToDouble(tm -> tm.getDiscount() * tm.getQty())
                    .sum();
            
            // Determine payment method and status
            String selectedPaymentMethod = cmbPaymentMethod.getValue();
            if (selectedPaymentMethod == null || selectedPaymentMethod.isEmpty()) {
                selectedPaymentMethod = "Cash";
            }
            
            String paymentMethod = selectedPaymentMethod.toUpperCase();
            String paymentStatus = "PAID";
            
            if ("CREDIT".equals(paymentMethod) || "CHEQUE".equals(paymentMethod)) {
                paymentStatus = "PENDING";
            }
            
            // Get customer paid amount and validate it's a valid number
            double customerPaid = 0.0;
            paidText = paidText.trim();
            try {
                customerPaid = Double.parseDouble(paidText);
                if (customerPaid < 0) {
                    new Alert(Alert.AlertType.WARNING, "Customer paid amount cannot be negative!").show();
                    txtCustomerPaid.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.WARNING, "Please enter a valid amount for customer paid!").show();
                txtCustomerPaid.requestFocus();
                return;
            }
            double totalCost = Double.parseDouble(txtTotal.getText().split(" /=")[0]);
            double balance = customerPaid - totalCost;
            
            // Create order detail
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setIssuedDate(LocalDateTime.now());
            orderDetail.setTotalCost(totalCost);
            orderDetail.setCustomerId(selectedCustomerId);
            orderDetail.setCustomerName(txtName.getText().trim().isEmpty() ? "Guest" : txtName.getText().trim());
            orderDetail.setDiscount(totalDiscount);
            orderDetail.setOperatorEmail(UserSessionData.email);
            orderDetail.setPaymentMethod(paymentMethod);
            orderDetail.setPaymentStatus(paymentStatus);
            orderDetail.setCustomerPaid(customerPaid);
            orderDetail.setBalance(balance);
            
            // Save order
            OrderDetail savedOrder = orderDetailService.saveOrderDetail(orderDetail);
            
            // Save order items (individual products in the order)
            List<OrderItem> orderItems = new ArrayList<>();
            for (CartTm tm : tms) {
                // Get product details to fetch product name
                ProductDetail productDetail = productDetailService.findProductDetailByCode(tm.getCode());
                
                // Use exact decimal quantity (e.g., 2.5 kg, 3.75 meters) - no rounding
                // Calculate item-level discount and totals
                double itemTotalDiscount = tm.getDiscount() * tm.getQty();
                
                OrderItem orderItem = OrderItem.builder()
                    .orderId(savedOrder.getCode())
                    .productCode(productDetail != null ? productDetail.getProductCode() : null)
                    .productName(tm.getDescription())
                    .batchCode(tm.getCode())
                    .batchNumber(productDetail != null ? productDetail.getBatchNumber() : null)
                    .quantity(tm.getQty()) // Store as decimal (supports 2.5, 3.75, etc.)
                    .unitPrice(tm.getSellingPrice())
                    .discountPerUnit(tm.getDiscount())
                    .totalDiscount(itemTotalDiscount)
                    .lineTotal(tm.getTotalCost())
                    .build();
                orderItems.add(orderItem);
            }
            orderItemService.saveAllOrderItems(orderItems);
            
            // Reduce stock immediately regardless of payment status so qty on hand is accurate
            // Use exact decimal quantity (supports 2.5, 3.75, etc.)
            for (CartTm tm : tms) {
                productDetailService.reduceStock(tm.getCode(), tm.getQty());
            }
            
            // Store the order code for printing later
            lastCompletedOrderCode = savedOrder.getCode();
            
            // Generate PDF for record keeping (no automatic printing)
            try {
                String receiptPath = pdfReportService.generateBillReceipt(savedOrder.getCode());
                
                if ("PAID".equals(paymentStatus)) {
                    new Alert(Alert.AlertType.CONFIRMATION, 
                        "Order Completed Successfully!\nPDF saved to: " + receiptPath + 
                        "\nClick Print button to print receipt.").show();
                } else {
                    new Alert(Alert.AlertType.INFORMATION, 
                        "Order created with " + paymentMethod + " payment. Status: PENDING.\nPDF saved to: " + receiptPath + 
                        "\nStock reduced immediately to reflect pending order.\nClick Print button to print receipt.").show();
                }
                
            } catch (Exception receiptEx) {
                receiptEx.printStackTrace();
                // Order was successful, just PDF generation failed
                if ("PAID".equals(paymentStatus)) {
                    new Alert(Alert.AlertType.WARNING, 
                        "Order completed but PDF generation failed: " + receiptEx.getMessage()).show();
                } else {
                    new Alert(Alert.AlertType.WARNING, 
                        "Order created but PDF generation failed: " + receiptEx.getMessage()).show();
                }
            }
            
            // Enable print button after order is completed (keep it enabled for printing)
            if (btnPrint != null) {
                btnPrint.setDisable(false);
            }
            
            clearFields();
            tms.clear();
            tblCart.setItems(tms);
            setTotal();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.WARNING, "Error completing order: " + e.getMessage()).show();
        }
    }
    
    private void clearFields() {
        txtContact.clear();
        txtName.clear();
        txtBarcode.clear();
        txtDescription.clear();
        txtSellingPrice.clear();
        txtDiscount.clear();
        txtQtyOnHand.clear();
        txtBuyingPrice.clear();
        txtQty.clear();
        txtCustomerPaid.clear();
        txtBalance.setText("0.00 /=");
        txtBalance.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: #dc2626;");
        selectedCustomerId = null;
        // Reset payment method to Cash
        if (cmbPaymentMethod != null) {
            cmbPaymentMethod.setValue("Cash");
        }
    }
    
    public void btnPrintReceipt(ActionEvent actionEvent) {
        try {
            if (lastCompletedOrderCode == null) {
                new Alert(Alert.AlertType.WARNING, "No order to print! Please complete an order first.").show();
                return;
            }
            
            // Printer feature moved to Feature/printer branch
            new Alert(Alert.AlertType.INFORMATION, "Printer feature has been moved to Feature/printer branch").show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).show();
        }
    }
}
