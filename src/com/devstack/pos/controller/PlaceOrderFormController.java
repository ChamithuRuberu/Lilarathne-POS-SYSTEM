package com.devstack.pos.controller;

import com.devstack.pos.entity.Customer;
import com.devstack.pos.entity.ItemDetail;
import com.devstack.pos.entity.LoyaltyCard;
import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.enums.CardType;
import com.devstack.pos.service.CustomerService;
import com.devstack.pos.service.ItemDetailService;
import com.devstack.pos.service.LoyaltyCardService;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.util.BarcodeGenerator;
import com.devstack.pos.util.UserSessionData;
import com.devstack.pos.view.tm.CartTm;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class PlaceOrderFormController {

    @FXML
    private AnchorPane context;

    @FXML
    private TextField txtEmail, txtName, txtContact, txtSalary, txtBarcode, txtDescription,
            txtSellingPrice, txtDiscount, txtShowPrice, txtQtyOnHand, txtBuyingPrice, txtQty;

    @FXML
    private Hyperlink urlNewLoyalty;

    @FXML
    private Label lblLoyaltyType, txtTotal, lblDiscountAvl;

    @FXML
    private TableView<CartTm> tblCart;

    @FXML
    private TableColumn<CartTm, String> colCode, colDesc;
    @FXML
    private TableColumn<CartTm, Double> colSelPrice, colSelDisc, colSelShPrice, colSelTotal;
    @FXML
    private TableColumn<CartTm, Integer> colSelQty;
    @FXML
    private TableColumn<CartTm, Button> colSelOperation;

    private final CustomerService customerService;
    private final ProductDetailService productDetailService;
    private final LoyaltyCardService loyaltyCardService;
    private final OrderDetailService orderDetailService;
    private final ItemDetailService itemDetailService;

    private final ObservableList<CartTm> tms = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Table columns
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colSelPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        colSelDisc.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colSelShPrice.setCellValueFactory(new PropertyValueFactory<>("showPrice"));
        colSelQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colSelTotal.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        colSelOperation.setCellValueFactory(new PropertyValueFactory<>("btn"));

        tblCart.setItems(tms);
    }

    @FXML
    void btnBackToHomeOnAction(ActionEvent event) throws IOException {
        setUi("DashboardForm", false);
    }

    @FXML
    void btnAddNewCustomerOnAction(ActionEvent event) throws IOException {
        setUi("CustomerForm", true);
    }

    @FXML
    void btnAddNewProductOnAction(ActionEvent event) throws IOException {
        setUi("ProductMainForm", true);
    }

    private void setUi(String url, boolean newStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/devstack/pos/view/" + url + ".fxml"));
        loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
        Scene scene = new Scene(loader.load());

        if (newStage) {
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
            stage.setMaximized(true);

        } else {
            Stage stage = (Stage) context.getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.setMaximized(true);

        }
    }

    @FXML
    void searchCustomer(ActionEvent event) {
        try {
            Customer customer = customerService.findCustomer(txtEmail.getText());
            if (customer != null) {
                txtName.setText(customer.getName());
                txtSalary.setText(String.valueOf(customer.getSalary()));
                txtContact.setText(customer.getContact());
                fetchLoyaltyCardData(txtEmail.getText());
            } else {
                clearCustomerFields();
                new Alert(Alert.AlertType.WARNING, "Customer not found!").show();
            }
        } catch (Exception e) {
            clearCustomerFields();
            new Alert(Alert.AlertType.ERROR, "Error fetching customer data!").show();
            e.printStackTrace();
        }
    }

    private void fetchLoyaltyCardData(String email) {
        urlNewLoyalty.setText("+ New Loyalty");
        urlNewLoyalty.setVisible(true);
    }

    @FXML
    void newLoyaltyOnAction(ActionEvent event) {
        try {
            double salary = Double.parseDouble(txtSalary.getText());
            CardType type;
            if (salary >= 100_000) type = CardType.PLATINUM;
            else if (salary >= 50_000) type = CardType.GOLD;
            else type = CardType.SILVER;

            String uniqueData = BarcodeGenerator.generateNumeric(12);

            Code128Writer barcodeWriter = new Code128Writer();
            BitMatrix bitMatrix = barcodeWriter.encode(uniqueData, BarcodeFormat.CODE_128, 300, 80);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "png", baos);

            if (urlNewLoyalty.getText().equals("+ New Loyalty")) {
                LoyaltyCard loyaltyCard = new LoyaltyCard();
                loyaltyCard.setCode((long) new Random().nextInt(10001));
                loyaltyCard.setCardType(type);
                loyaltyCard.setBarcode(Base64.getEncoder().encodeToString(baos.toByteArray()));
                loyaltyCard.setEmail(txtEmail.getText());
                loyaltyCardService.saveLoyaltyCard(loyaltyCard);
                new Alert(Alert.AlertType.CONFIRMATION, "Loyalty card saved successfully!").show();
                urlNewLoyalty.setText("Show Loyalty Card Info");
            }
        } catch ( IOException e) {
            new Alert(Alert.AlertType.ERROR, "Error generating barcode!").show();
            e.printStackTrace();
        }
    }

    @FXML
    void loadProduct(ActionEvent event) {
        try {
            ProductDetail productDetail = productDetailService.findByCodeWithProduct(txtBarcode.getText());
            if (productDetail != null) {
//                txtDescription.setText(productDetail.getProduct().getDescription());
                txtDiscount.setText("250");
                txtSellingPrice.setText(String.valueOf(productDetail.getSellingPrice()));
                txtShowPrice.setText(String.valueOf(productDetail.getShowPrice()));
                txtQtyOnHand.setText(String.valueOf(productDetail.getQtyOnHand()));
                txtBuyingPrice.setText(String.valueOf(productDetail.getBuyingPrice()));
                txtQty.requestFocus();
            } else {
                new Alert(Alert.AlertType.WARNING, "Product not found!").show();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error fetching product!").show();
            e.printStackTrace();
        }
    }

    @FXML
    void addToCart(ActionEvent event) {
        try {
            int qty = Integer.parseInt(txtQty.getText());
            double discount = Double.parseDouble(txtDiscount.getText());
            double sellingPrice = Double.parseDouble(txtSellingPrice.getText()) - discount;
            double totalCost = qty * sellingPrice;

            CartTm existing = tms.stream().filter(tm -> tm.getCode().equals(txtBarcode.getText())).findFirst().orElse(null);

            if (existing != null) {
                existing.setQty(existing.getQty() + qty);
                existing.setTotalCost(existing.getTotalCost() + totalCost);
                tblCart.refresh();
            } else {
                Button btn = new Button("Remove");
                CartTm tm = new CartTm(txtBarcode.getText(),
                        txtDescription.getText(),
                        discount,
                        sellingPrice,
                        Double.parseDouble(txtShowPrice.getText()),
                        qty,
                        totalCost,
                        btn);

                btn.setOnAction(e -> {
                    tms.remove(tm);
                    tblCart.refresh();
                    setTotal();
                });

                tms.add(tm);
                clearProductFields();
                tblCart.refresh();
            }
            setTotal();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Invalid quantity!").show();
        }
    }

    private void clearProductFields() {
        txtDescription.clear();
        txtSellingPrice.clear();
        txtDiscount.clear();
        txtShowPrice.clear();
        txtQtyOnHand.clear();
        txtBuyingPrice.clear();
        txtQty.clear();
        txtBarcode.clear();
        txtBarcode.requestFocus();
    }

    private void clearCustomerFields() {
        txtName.clear();
        txtSalary.clear();
        txtContact.clear();
        urlNewLoyalty.setVisible(false);
    }

    private void setTotal() {
        double total = tms.stream().mapToDouble(CartTm::getTotalCost).sum();
        txtTotal.setText(total + " /=");
    }

    @FXML
    void btnCompleteOrder(ActionEvent event) {
        try {
            if (tms.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Cart is empty!").show();
                return;
            }

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setIssuedDate(LocalDateTime.now());
            orderDetail.setTotalCost(tms.stream().mapToDouble(CartTm::getTotalCost).sum());
            orderDetail.setDiscount(tms.stream().mapToDouble(CartTm::getDiscount).sum());
            orderDetail.setOperatorEmail(UserSessionData.email);

            List<ItemDetail> itemDetails = new ArrayList<>();
            for (CartTm tm : tms) {
                ItemDetail item = new ItemDetail();
                item.setDetailCode(tm.getCode());
                item.setQty(tm.getQty());
                item.setDiscount(tm.getDiscount());
                item.setAmount(tm.getTotalCost());
                itemDetails.add(item);
            }

            orderDetailService.createOrder(orderDetail, itemDetails);

            new Alert(Alert.AlertType.CONFIRMATION, "Order completed successfully!").show();

            tms.clear();
            tblCart.refresh();
            clearFields();
            setTotal();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error completing order!").show();
        }
    }

    private void clearFields() {
        txtEmail.clear();
        clearCustomerFields();
        clearProductFields();
        urlNewLoyalty.setText("+ New Loyalty");
        urlNewLoyalty.setVisible(false);
    }
}
