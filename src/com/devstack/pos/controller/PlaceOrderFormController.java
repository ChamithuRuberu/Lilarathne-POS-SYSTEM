package com.devstack.pos.controller;

import com.devstack.pos.entity.Customer;
import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.OrderItem;
import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.service.CustomerService;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.OrderItemService;
import com.devstack.pos.service.PDFReportService;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.ProductService;
import com.devstack.pos.util.AuthorizationUtil;
import com.devstack.pos.util.ReceiptPrinter;
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
    @FXML
    public TabPane tabPane;

    private Long selectedCustomerId = null;
    private Long lastCompletedOrderCode = null;
    private final CustomerService customerService;
    private final ProductDetailService productDetailService;
    private final ProductService productService;
    private final OrderDetailService orderDetailService;
    private final OrderItemService orderItemService;
    private final PDFReportService pdfReportService;
    private final ReceiptPrinter receiptPrinter;
    private Timeline barcodeDebounceTimeline;
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
        
        // Setup barcode scanner detection for automatic product loading
        setupBarcodeScannerDetection();
        
        // Track tab changes to determine order type
        if (tabPane != null) {
            tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
                // Order type will be determined when completing order based on active tab
            });
            
            // Remove black bar from TabPane header - run after scene is shown
            tabPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    javafx.application.Platform.runLater(() -> {
                        removeTabPaneBlackBar();
                    });
                }
            });
            
            // Also try immediately
            javafx.application.Platform.runLater(() -> {
                removeTabPaneBlackBar();
            });
        }
    }
    
    private void removeTabPaneBlackBar() {
        if (tabPane == null) return;
        
        try {
            // Try multiple lookup attempts with retries
            for (int i = 0; i < 3; i++) {
                javafx.scene.Node headerArea = tabPane.lookup(".tab-header-area");
                if (headerArea != null) {
                    headerArea.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-background-insets: 0;");
                }
                
                javafx.scene.Node headerBackground = tabPane.lookup(".tab-header-background");
                if (headerBackground != null) {
                    headerBackground.setStyle("-fx-background-color: transparent; -fx-opacity: 0; -fx-pref-height: 0; -fx-max-height: 0; -fx-min-height: 0;");
                    // Also try to hide it completely
                    if (headerBackground instanceof javafx.scene.layout.Region) {
                        ((javafx.scene.layout.Region) headerBackground).setVisible(false);
                    }
                }
                
                // Try to find any StackPane or Region in the header
                javafx.scene.Node headersRegion = tabPane.lookup(".headers-region");
                if (headersRegion != null) {
                    headersRegion.setStyle("-fx-background-color: transparent;");
                }
                
                // Wait a bit before retry
                if (i < 2) {
                    Thread.sleep(50);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getCurrentOrderType() {
        if (tabPane != null && tabPane.getSelectionModel().getSelectedItem() != null) {
            String tabText = tabPane.getSelectionModel().getSelectedItem().getText();
            // Check if tab text contains "Construction" (handles emoji prefix)
            if (tabText != null && tabText.toLowerCase().contains("construction")) {
                return "CONSTRUCTION";
            }
            // Alternatively, check by tab index (index 0 = Hardware, index 1 = Construction)
            int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();
            if (selectedIndex == 1) {
                return "CONSTRUCTION";
            }
        }
        return "HARDWARE"; // Default to Hardware
    }
    
    @Override
    protected String getCurrentPageName() {
        return "Place Order";
    }

    public void btnBackToHomeOnAction(ActionEvent actionEvent) {
        btnDashboardOnAction(actionEvent);
    }

    public void btnAddNewCustomerOnAction(ActionEvent actionEvent) throws IOException {
        setUi("CustomerForm", true);
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
            
            Customer customer = customerService.findByContact(contact);
            if (customer != null) {
                selectedCustomerId = customer.getId();
                txtName.setText(customer.getName());
                new Alert(Alert.AlertType.INFORMATION, "Customer found: " + customer.getName()).show();
            } else {
                selectedCustomerId = null;
                txtName.clear();
                new Alert(Alert.AlertType.WARNING, "Customer not found! You can proceed with guest checkout or add a new customer.").show();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error searching customer: " + e.getMessage()).show();
            e.printStackTrace();
        }
    }

    /**
     * Sets up barcode scanner detection with debounced text change listener
     * This allows barcode scanners to automatically trigger product loading
     */
    private void setupBarcodeScannerDetection() {
        // Add comprehensive event listeners for debugging
        txtBarcode.setOnKeyPressed(event -> {
            System.out.println("[PLACE ORDER DEBUG] ========== KEY PRESSED ==========");
            System.out.println("[PLACE ORDER DEBUG] Key code: " + event.getCode());
            System.out.println("[PLACE ORDER DEBUG] Key text: " + event.getText());
            System.out.println("[PLACE ORDER DEBUG] Current field text: '" + txtBarcode.getText() + "'");
        });
        
        txtBarcode.setOnKeyTyped(event -> {
            System.out.println("[PLACE ORDER DEBUG] ========== KEY TYPED ==========");
            System.out.println("[PLACE ORDER DEBUG] Character: '" + event.getCharacter() + "'");
            System.out.println("[PLACE ORDER DEBUG] Current field text: '" + txtBarcode.getText() + "'");
        });
        
        // Setup debounced text change listener for barcode scanners
        // Barcode scanners typically send data very quickly (all at once), so we use a delay
        // to ensure we capture the complete barcode before processing
        barcodeDebounceTimeline = new Timeline(new KeyFrame(Duration.millis(200), e -> {
            System.out.println("[PLACE ORDER DEBUG] Debounce timer fired");
            // Don't trigger if we're updating the barcode programmatically
            if (isUpdatingBarcodeProgrammatically) {
                System.out.println("[PLACE ORDER DEBUG] Skipping - updating programmatically");
                return;
            }
            String barcode = txtBarcode.getText();
            System.out.println("[PLACE ORDER DEBUG] Current barcode text: '" + barcode + "' (length: " + (barcode != null ? barcode.length() : 0) + ")");
            if (barcode != null && !barcode.trim().isEmpty()) {
                // Remove any newline/carriage return characters that barcode scanners might append
                String originalBarcode = barcode;
                barcode = barcode.replace("\n", "").replace("\r", "").trim();
                if (!barcode.equals(originalBarcode)) {
                    System.out.println("[PLACE ORDER DEBUG] Removed newline characters. Original: '" + originalBarcode + "', Cleaned: '" + barcode + "'");
                }
                if (!barcode.isEmpty()) {
                    // Auto-trigger product loading after debounce delay
                    // This ensures we capture complete barcode from scanner
                    System.out.println("[PLACE ORDER DEBUG] ✓ Auto-loading product for barcode: " + barcode);
                    loadProduct(null); // Call loadProduct automatically
                } else {
                    System.out.println("[PLACE ORDER DEBUG] Barcode is empty after cleaning");
                }
            } else {
                System.out.println("[PLACE ORDER DEBUG] Barcode is null or empty");
            }
        }));
        barcodeDebounceTimeline.setCycleCount(1);
        
        // Listen for text changes to restart debounce timer
        txtBarcode.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("[PLACE ORDER DEBUG] ========== TEXT PROPERTY CHANGED ==========");
            System.out.println("[PLACE ORDER DEBUG] Old value: '" + (oldValue != null ? oldValue : "null") + "'");
            System.out.println("[PLACE ORDER DEBUG] New value: '" + (newValue != null ? newValue : "null") + "'");
            System.out.println("[PLACE ORDER DEBUG] Is updating programmatically: " + isUpdatingBarcodeProgrammatically);
            
            // Don't start debounce timer if we're updating programmatically
            if (isUpdatingBarcodeProgrammatically) {
                System.out.println("[PLACE ORDER DEBUG] Skipping - updating programmatically");
                return;
            }
            
            System.out.println("[PLACE ORDER DEBUG] Processing text change...");
            
            // Reset and restart the debounce timer when text changes
            if (barcodeDebounceTimeline != null) {
                System.out.println("[PLACE ORDER DEBUG] Restarting debounce timer (200ms delay)");
                barcodeDebounceTimeline.stop();
                barcodeDebounceTimeline.playFromStart();
            }
        });
        
        // Auto-focus barcode field when form loads
        javafx.application.Platform.runLater(() -> {
            txtBarcode.requestFocus();
            System.out.println("[PLACE ORDER DEBUG] Barcode field focused and ready for scanning");
            System.out.println("[PLACE ORDER DEBUG] Field is editable: " + txtBarcode.isEditable());
            System.out.println("[PLACE ORDER DEBUG] Field is disabled: " + txtBarcode.isDisabled());
            System.out.println("[PLACE ORDER DEBUG] Field is visible: " + txtBarcode.isVisible());
            System.out.println("[PLACE ORDER DEBUG] Field has focus: " + txtBarcode.isFocused());
            System.out.println("[PLACE ORDER DEBUG] ==========================================");
            System.out.println("[PLACE ORDER DEBUG] TEST: Try typing manually in the barcode field first");
            System.out.println("[PLACE ORDER DEBUG] Then try scanning a barcode...");
            System.out.println("[PLACE ORDER DEBUG] ==========================================");
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
                // Ensure barcode field holds the batch code used for stock reduction
                // Set flag to prevent debounce timer from triggering during programmatic update
                isUpdatingBarcodeProgrammatically = true;
                try {
                    txtBarcode.setText(productDetail.getCode());
                } finally {
                    javafx.application.Platform.runLater(() -> {
                        javafx.application.Platform.runLater(() -> {
                            isUpdatingBarcodeProgrammatically = false;
                        });
                    });
                }
                txtSellingPrice.setText(String.valueOf(productDetail.getSellingPrice()));
                txtQtyOnHand.setText(String.valueOf(productDetail.getQtyOnHand()));
                txtBuyingPrice.setText(String.valueOf(productDetail.getBuyingPrice()));
                txtQty.requestFocus();
            } else {
                new Alert(Alert.AlertType.WARNING, "Can't Find the Product!").show();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.WARNING, "Can't Find the Product!").show();
            e.printStackTrace();
        }
    }

    ObservableList<CartTm> tms = FXCollections.observableArrayList();

    public void addToCart(ActionEvent actionEvent) {
        int qty = Integer.parseInt(txtQty.getText());
        double unitPrice = Double.parseDouble(txtSellingPrice.getText());
        double unitDiscount = 0.0;
        String discountText = txtDiscount.getText() == null ? "" : txtDiscount.getText().trim();
        if (!discountText.isEmpty()) {
            unitDiscount = Double.parseDouble(discountText);
        }
        double effectiveUnitPrice = unitPrice - unitDiscount;
        if (effectiveUnitPrice < 0) effectiveUnitPrice = 0.0;
        double totalCost = qty * effectiveUnitPrice;

        // Disable print button when starting a new order (first item added to empty cart)
        if (btnPrint != null && tms.isEmpty()) {
            btnPrint.setDisable(true);
            lastCompletedOrderCode = null;
        }

        CartTm selectedCartTm = isExists(txtBarcode.getText());
        if (selectedCartTm != null) {
            selectedCartTm.setQty(qty + selectedCartTm.getQty());
            selectedCartTm.setTotalCost(totalCost + selectedCartTm.getTotalCost());
            tblCart.refresh();
        } else {
            Button btn = new Button("Remove");
            CartTm tm = new CartTm(txtBarcode.getText(),
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
        for (CartTm tm : tms
        ) {
            total += tm.getTotalCost();
        }
        txtTotal.setText(total + " /=");
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
            orderDetail.setOrderType(getCurrentOrderType()); // Set order type based on active tab
            orderDetail.setCustomerPaid(customerPaid);
            orderDetail.setBalance(balance);
            
            // Save order
            OrderDetail savedOrder = orderDetailService.saveOrderDetail(orderDetail);
            
            // Save order items (individual products in the order)
            List<OrderItem> orderItems = new ArrayList<>();
            for (CartTm tm : tms) {
                // Get product details to fetch product name
                ProductDetail productDetail = productDetailService.findProductDetailByCode(tm.getCode());
                
                OrderItem orderItem = OrderItem.builder()
                    .orderId(savedOrder.getCode())
                    .productCode(productDetail != null ? productDetail.getProductCode() : null)
                    .productName(tm.getDescription())
                    .batchCode(tm.getCode())
                    .batchNumber(productDetail != null ? productDetail.getBatchNumber() : null)
                    .quantity(tm.getQty())
                    .unitPrice(tm.getSellingPrice())
                    .discountPerUnit(tm.getDiscount())
                    .totalDiscount(tm.getDiscount() * tm.getQty())
                    .lineTotal(tm.getTotalCost())
                    .build();
                orderItems.add(orderItem);
            }
            orderItemService.saveAllOrderItems(orderItems);
            
            // Reduce stock immediately regardless of payment status so qty on hand is accurate
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
            
            // Generate plain text receipt for thermal printer
            String receiptText = pdfReportService.generatePlainTextReceipt(lastCompletedOrderCode);
            
            // Print receipt directly to thermal printer using ESC/POS commands
            boolean printed = receiptPrinter.printRawText(receiptText);
            
            if (printed) {
                new Alert(Alert.AlertType.INFORMATION, "Receipt printed successfully!").show();
            } else {
                new Alert(Alert.AlertType.WARNING, "Printing failed. Please check printer connection.").show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error printing receipt: " + e.getMessage()).show();
        }
    }
}
