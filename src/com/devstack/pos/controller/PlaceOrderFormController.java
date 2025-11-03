package com.devstack.pos.controller;

import com.devstack.pos.entity.Customer;
import com.devstack.pos.entity.ItemDetail;
import com.devstack.pos.entity.LoyaltyCard;
import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.enums.CardType;
import com.devstack.pos.service.CustomerService;
import com.devstack.pos.service.ItemDetailService;
import com.devstack.pos.service.LoyaltyCardService;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.ProductService;
import com.devstack.pos.util.AuthorizationUtil;
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
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
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
public class PlaceOrderFormController extends BaseController {
    public TextField txtEmail;
    public TextField txtName;
    public Hyperlink urlNewLoyalty;
    public Label lblLoyaltyType;
    public TextField txtContact;
    public TextField txtSalary;
    public TextField txtBarcode;
    public TextField txtDescription;
    public TextField txtSellingPrice;
    public TextField txtDiscount;
    public TextField txtShowPrice;
    public TextField txtQtyOnHand;
    public Label lblDiscountAvl;
    public TextField txtBuyingPrice;
    public TextField txtQty;
    public TableView<CartTm> tblCart;
    public TableColumn colCode;
    public TableColumn colDesc;
    public TableColumn colSelPrice;
    public TableColumn colSelDisc;
    public TableColumn colSelShPrice;
    public TableColumn colSelQty;
    public TableColumn colSelTotal;
    public TableColumn colSelOperation;
    public Text txtTotal;

    private final CustomerService customerService;
    private final ProductDetailService productDetailService;
    private final ProductService productService;
    private final LoyaltyCardService loyaltyCardService;
    private final OrderDetailService orderDetailService;
    private final ItemDetailService itemDetailService;


    public void initialize() {
        // Initialize sidebar
        initializeSidebar();
        
        // Authorization check: POS Orders accessible by ADMIN and CASHIER
        if (!AuthorizationUtil.canAccessPOSOrders()) {
            AuthorizationUtil.showUnauthorizedAlert();
            btnBackToHomeOnAction(null);
            return;
        }
        
        // Initialize table columns
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colSelPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        colSelDisc.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colSelShPrice.setCellValueFactory(new PropertyValueFactory<>("showPrice"));
        colSelQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colSelTotal.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        colSelOperation.setCellValueFactory(new PropertyValueFactory<>("btn"));
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
            Customer customer = customerService.findCustomer(txtEmail.getText());
            if (customer != null) {
                txtName.setText(customer.getName());
                txtSalary.setText(String.valueOf(customer.getSalary()));
                txtContact.setText(customer.getContact());
                fetchLoyaltyCardData(txtEmail.getText());
            } else {
                new Alert(Alert.AlertType.WARNING, "Can't Find the Customer!").show();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.WARNING, "Can't Find the Customer!").show();
            e.printStackTrace();
        }
    }

    private void fetchLoyaltyCardData(String email) {
        urlNewLoyalty.setText("+ New Loyalty");
        urlNewLoyalty.setVisible(true);
    }

    public void newLoyaltyOnAction(ActionEvent actionEvent) {
        try {
            double salary = Double.parseDouble(txtSalary.getText());

            CardType type = null;
            if (salary >= 100000) {
                type = CardType.PLATINUM;
            } else if (salary >= 50000) {
                type = CardType.GOLD;
            } else {
                type = CardType.SILVER;
            }

            // Generate unique numeric barcode for loyalty card
            String uniqueData = BarcodeGenerator.generateNumeric(12);
            
            // Generate CODE 128 barcode (standard POS barcode format)
            Code128Writer barcodeWriter = new Code128Writer();
            BitMatrix bitMatrix = barcodeWriter.encode(
                    uniqueData,
                    BarcodeFormat.CODE_128,
                    300,  // width
                    80    // height (barcode height)
            );
            
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "png", baos);
            byte[] arr = baos.toByteArray();

            if (urlNewLoyalty.getText().equals("+ New Loyalty")) {
                LoyaltyCard loyaltyCard = new LoyaltyCard();
                loyaltyCard.setCode((long) new Random().nextInt(10001));
                loyaltyCard.setCardType(type);
                loyaltyCard.setBarcode(Base64.getEncoder().encodeToString(arr));
                loyaltyCard.setEmail(txtEmail.getText());
                
                loyaltyCardService.saveLoyaltyCard(loyaltyCard);
                new Alert(Alert.AlertType.CONFIRMATION, "Saved").show();
                urlNewLoyalty.setText("Show Loyalty Card Info");
            } else {
                // view data
            }
        } catch ( IOException e) {
            new Alert(Alert.AlertType.ERROR, "Error generating barcode: " + e.getMessage()).show();
            e.printStackTrace();
        } catch (Exception e) {
            new Alert(Alert.AlertType.WARNING, "Try Again Shortly!").show();
            e.printStackTrace();
        }
    }

    public void loadProduct(ActionEvent actionEvent) {
        try {
            // Search product detail by barcode code (code is the barcode identifier)
            ProductDetail productDetail = productDetailService.findByCodeWithProduct(txtBarcode.getText());
            if (productDetail != null) {
                // Load product description using product code
                Product product = productService.findProduct(productDetail.getProductCode());
                if (product != null) {
                    txtDescription.setText(product.getDescription());
                }
                txtDiscount.setText(String.valueOf(250));
                txtSellingPrice.setText(String.valueOf(productDetail.getSellingPrice()));
                txtShowPrice.setText(String.valueOf(productDetail.getShowPrice()));
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
        /*if (customer.cardType.equals(CardType.GOLD.name())){
            //
        }*/
        double discount = 250;//=>

        double sellingPrice = (Double.parseDouble(txtSellingPrice.getText())-discount);
        double totalCost = qty * sellingPrice;


        CartTm selectedCartTm = isExists(txtBarcode.getText());
        if (selectedCartTm != null) {
            selectedCartTm.setQty(qty + selectedCartTm.getQty());
            selectedCartTm.setTotalCost(totalCost + selectedCartTm.getTotalCost());
            tblCart.refresh();
        } else {
            Button btn = new Button("Remove");
            CartTm tm = new CartTm(txtBarcode.getText(),
                    txtDescription.getText(),
                    Double.parseDouble(txtDiscount.getText()),
                    sellingPrice,
                    Double.parseDouble(txtShowPrice.getText()),
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
        txtShowPrice.clear();
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
            
            // Calculate total discount
            double totalDiscount = tms.stream()
                    .mapToDouble(CartTm::getDiscount)
                    .sum();
            
            // Create order detail
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setIssuedDate(LocalDateTime.now());
            orderDetail.setTotalCost(Double.parseDouble(txtTotal.getText().split(" /=")[0]));
            orderDetail.setCustomerEmail(txtEmail.getText());
            orderDetail.setDiscount(totalDiscount);
            orderDetail.setOperatorEmail(UserSessionData.email);
            
            // Create item details from cart
            List<ItemDetail> itemDetails = new ArrayList<>();
            for (CartTm tm : tms) {
                ItemDetail itemDetail = new ItemDetail();
                itemDetail.setDetailCode(tm.getCode());
                itemDetail.setQty(tm.getQty());
                itemDetail.setDiscount(tm.getDiscount());
                itemDetail.setAmount(tm.getTotalCost());
                itemDetails.add(itemDetail);
            }
            
            // Save order with items
            orderDetailService.createOrder(orderDetail, itemDetails);
            itemDetailService.saveItemDetails(itemDetails);
            
            new Alert(Alert.AlertType.CONFIRMATION, "Order Completed Successfully!").show();
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
        txtEmail.clear();
        txtName.clear();
        txtContact.clear();
        txtSalary.clear();
        txtBarcode.clear();
        txtDescription.clear();
        txtSellingPrice.clear();
        txtDiscount.clear();
        txtShowPrice.clear();
        txtQtyOnHand.clear();
        txtBuyingPrice.clear();
        txtQty.clear();
        urlNewLoyalty.setText("+ New Loyalty");
        urlNewLoyalty.setVisible(false);
    }
}
