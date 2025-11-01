package com.devstack.pos.controller;

import com.devstack.pos.entity.Product;
import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.service.ProductService;
import com.devstack.pos.view.tm.ProductDetailTm;
import com.devstack.pos.view.tm.ProductTm;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ProductMainPageController {
    public TextArea txtProductDescription;
    public Button btnSaveUpdate;
    public TextField txtProductCode;
    public TableView<ProductTm> tbl;
    public TableColumn colProductId;
    public TableColumn colProductDesc;
    public TableColumn colProductShowMore;
    public TableColumn colProductDelete;
    public TextField txtSelectedProdId;
    public TextArea txtSelectedProdDescription;
    public Button btnNewBatch;
    public TableView<ProductDetailTm> tblDetail;
    public TableColumn colPDId;
    public TableColumn colPDQty;
    public TableColumn colPDSellingPrice;
    public TableColumn colPDBuyingPrice;
    public TableColumn colPDDAvailability;
    public TableColumn colPDShowPrice;
    public TableColumn colPDDelete;
    public AnchorPane context;

    private final ProductService productService;
    private final ProductDetailService productDetailService;

    private String searchText = "";

    public void initialize() {
        colProductId.setCellValueFactory(new PropertyValueFactory<>("code"));
        colProductDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colProductShowMore.setCellValueFactory(new PropertyValueFactory<>("showMore"));
        colProductDelete.setCellValueFactory(new PropertyValueFactory<>("delete"));

        colPDId.setCellValueFactory(new PropertyValueFactory<>("code"));
        colPDQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        colPDSellingPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        colPDBuyingPrice.setCellValueFactory(new PropertyValueFactory<>("buyingPrice"));
        colPDDAvailability.setCellValueFactory(new PropertyValueFactory<>("discountAvailability"));
        colPDShowPrice.setCellValueFactory(new PropertyValueFactory<>("showPrice"));
        colPDDelete.setCellValueFactory(new PropertyValueFactory<>("delete"));

        //--- load new Product Id
        loadProductId();
        loadAllProducts(searchText);
        //--- load new Product Id

        tbl.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        setData(newValue);
                    }
                });
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

    private void setData(ProductTm newValue) {
        txtSelectedProdId.setText(String.valueOf(newValue.getCode()));
        txtSelectedProdDescription.setText(newValue.getDescription());
        btnNewBatch.setDisable(false);
        try {
            loadBatchData(newValue.getCode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadProductId() {
        try {
            int lastId = productService.getLastProductId();
            txtProductCode.setText(String.valueOf(lastId + 1));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void btnBackToHomeOnAction(ActionEvent actionEvent) throws IOException {
        setUi("DashboardForm");
    }

    public void btnNewProductOnAction(ActionEvent actionEvent) {
        try {
            Product product = new Product();
            product.setCode(Integer.parseInt(txtProductCode.getText()));
            product.setDescription(txtProductDescription.getText());

            if (btnSaveUpdate.getText().equals("Save Product")) {
                productService.saveProduct(product);
                new Alert(Alert.AlertType.CONFIRMATION, "Product Saved!").show();
                clearFields();
                loadAllProducts(searchText);
            } else {
                if (productService.updateProduct(product)) {
                    new Alert(Alert.AlertType.CONFIRMATION, "Product Updated!").show();
                    clearFields();
                    loadAllProducts(searchText);
                    btnSaveUpdate.setText("Save Product");
                } else {
                    new Alert(Alert.AlertType.WARNING, "Try Again!").show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    private void loadAllProducts(String searchText) {
        try {
            ObservableList<ProductTm> tms = FXCollections.observableArrayList();
            for (Product product : productService.findAllProducts()) {
                Button showMore = new Button("Show more");
                Button delete = new Button("Delete");
                ProductTm tm = new ProductTm(product.getCode(), product.getDescription(), showMore, delete);
                tms.add(tm);
                
                delete.setOnAction((e) -> {
                    try {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                                "Are you sure?", ButtonType.YES, ButtonType.NO);
                        alert.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.YES) {
                                if (productService.deleteProduct(product.getCode())) {
                                    new Alert(Alert.AlertType.CONFIRMATION, "Product Deleted!").show();
                                    loadAllProducts(searchText);
                                } else {
                                    new Alert(Alert.AlertType.WARNING, "Try Again!").show();
                                }
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, ex.getMessage()).show();
                    }
                });
            }
            tbl.setItems(tms);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error loading products: " + e.getMessage()).show();
        }
    }

    private void clearFields() {
        txtProductCode.clear();
        txtProductDescription.clear();
        loadProductId();
    }

    public void btnAddNewOnAction(ActionEvent actionEvent) {
    }

    public void newBatchOnAction(ActionEvent actionEvent) throws IOException {
        loadExternalUi(false, null);
    }

    private void loadExternalUi(boolean state, ProductDetailTm tm) throws IOException {
        if (!txtSelectedProdId.getText().isEmpty()) {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/devstack/pos/view/NewBatchForm.fxml"));
            loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
            Parent parent = loader.load();
            NewBatchFormController controller = loader.getController();
            controller.setDetails(Integer.parseInt(txtSelectedProdId.getText()),
                    txtSelectedProdDescription.getText(), stage, state, tm);
            stage.setScene(new Scene(parent));
            stage.show();
            stage.centerOnScreen();
        } else {
            new Alert(Alert.AlertType.WARNING, "Please select a valid one!").show();
        }
    }

    private void loadBatchData(int code) {
        try {
            ObservableList<ProductDetailTm> obList = FXCollections.observableArrayList();
            for (ProductDetail productDetail : productDetailService.findByProductCode(code)) {
                Button btn = new Button("Delete");
                ProductDetailTm tm = new ProductDetailTm(
                        productDetail.getCode(),
                        productDetail.getQtyOnHand(),
                        productDetail.getSellingPrice(),
                        productDetail.getBuyingPrice(),
                        productDetail.isDiscountAvailability(),
                        productDetail.getShowPrice(),
                        btn
                );
                obList.add(tm);
                
                btn.setOnAction((e) -> {
                    try {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                                "Are you sure?", ButtonType.YES, ButtonType.NO);
                        alert.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.YES) {
                                if (productDetailService.deleteProductDetail(productDetail.getCode())) {
                                    new Alert(Alert.AlertType.CONFIRMATION, "Product Detail Deleted!").show();
                                    loadBatchData(code);
                                } else {
                                    new Alert(Alert.AlertType.WARNING, "Try Again!").show();
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

    private void setUi(String url) throws IOException {
        Stage stage = (Stage) context.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/com/devstack/pos/view/" + url + ".fxml"));
        loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
        stage.setScene(new Scene(loader.load()));
        stage.centerOnScreen();
    }
}
