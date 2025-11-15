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
import javafx.stage.Stage;
import javafx.fxml.FXML;
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
    public JFXComboBox<String> cmbPaymentMethod;

    private Long selectedCustomerId = null;
    private final CustomerService customerService;
    private final ProductDetailService productDetailService;
    private final ProductService productService;
    private final OrderDetailService orderDetailService;
    private final OrderItemService orderItemService;
    private final PDFReportService pdfReportService;
    private final ReceiptPrinter receiptPrinter;


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
                txtBarcode.setText(productDetail.getCode());
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
    }

    public void btnCompleteOrder(ActionEvent actionEvent) {
        try {
            if (tms.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Cart is empty!").show();
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
            
            // Create order detail
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setIssuedDate(LocalDateTime.now());
            orderDetail.setTotalCost(Double.parseDouble(txtTotal.getText().split(" /=")[0]));
            orderDetail.setCustomerId(selectedCustomerId);
            orderDetail.setCustomerName(txtName.getText().trim().isEmpty() ? "Guest" : txtName.getText().trim());
            orderDetail.setDiscount(totalDiscount);
            orderDetail.setOperatorEmail(UserSessionData.email);
            orderDetail.setPaymentMethod(paymentMethod);
            orderDetail.setPaymentStatus(paymentStatus);
            orderDetail.setOrderType("HARDWARE"); // Always set to HARDWARE
            
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
            
            // Only reduce stock if payment is CASH (PAID)
            // For CREDIT/CHEQUE (PENDING), stock will be reduced when payment is completed
            if ("PAID".equals(paymentStatus)) {
                for (CartTm tm : tms) {
                    productDetailService.reduceStock(tm.getCode(), tm.getQty());
                }
                
                // Generate and print bill receipt
                try {
                    String receiptPath = pdfReportService.generateBillReceipt(savedOrder.getCode());
                    
                    // Print receipt and open cash drawer
                    receiptPrinter.printBillAndOpenDrawer(receiptPath);
                    
                    new Alert(Alert.AlertType.CONFIRMATION, 
                        "Order Completed Successfully!\nReceipt saved to: " + receiptPath).show();
                    
                } catch (Exception receiptEx) {
                    receiptEx.printStackTrace();
                    // Order was successful, just receipt printing failed
                    new Alert(Alert.AlertType.WARNING, 
                        "Order completed but receipt printing failed: " + receiptEx.getMessage()).show();
                }
                
            } else {
                new Alert(Alert.AlertType.INFORMATION, 
                    "Order created with " + paymentMethod + " payment. Status: PENDING. Stock will be reduced when payment is completed.").show();
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
        selectedCustomerId = null;
        // Reset payment method to Cash
        if (cmbPaymentMethod != null) {
            cmbPaymentMethod.setValue("Cash");
        }
    }
}
