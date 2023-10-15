package com.devstack.pos.controller;

import com.devstack.pos.bo.BoFactory;
import com.devstack.pos.bo.custom.ProductBo;
import com.devstack.pos.bo.custom.ProductDetailBo;
import com.devstack.pos.bo.custom.impl.ProductBoImpl;
import com.devstack.pos.dto.CustomerDto;
import com.devstack.pos.dto.ProductDetailDto;
import com.devstack.pos.dto.ProductDto;
import com.devstack.pos.enums.BoType;
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

import java.io.IOException;
import java.sql.SQLException;

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


    ProductBo bo = BoFactory.getInstance().getBo(BoType.PRODUCT);
    ProductDetailBo detailBo = BoFactory.getInstance().getBo(BoType.PRODUCT_DETAIL);


    private String searchText = "";

    public void initialize() throws SQLException, ClassNotFoundException {


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
                    setData(newValue);
                });
        tblDetail.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    try {
                        loadExternalUi(true, newValue);
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
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadProductId() {
        try {
            txtProductCode.setText(String.valueOf(bo.getLastProductId()));
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public void btnBackToHomeOnAction(ActionEvent actionEvent) throws IOException {
        setUi("DashboardForm");
    }

    public void btnNewProductOnAction(ActionEvent actionEvent) {
        try {
            if (btnSaveUpdate.getText().equals("Save Product")) {

                if (bo.saveProduct(new ProductDto(Integer.parseInt(txtProductCode.getText()), txtProductDescription.getText()))) {
                    new Alert(Alert.AlertType.CONFIRMATION, "Product Saved!").show();
                    clearFields();
                    loadAllProducts(searchText);
                } else {
                    new Alert(Alert.AlertType.WARNING, "Try Again!").show();
                }
            } else {
                if (bo.saveProduct(new ProductDto(Integer.parseInt(txtProductCode.getText()), txtProductDescription.getText()))) {
                    new Alert(Alert.AlertType.CONFIRMATION, "Product Updated!").show();
                    clearFields();
                    loadAllProducts(searchText);
                    //---------
                    btnSaveUpdate.setText("Save Product");
                } else {
                    new Alert(Alert.AlertType.WARNING, "Try Again!").show();
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }

    }

    private void loadAllProducts(String searchText) throws SQLException, ClassNotFoundException {
        ObservableList<ProductTm> tms = FXCollections.observableArrayList();
        for (ProductDto dto : bo.findAllProducts()
        ) {
            Button showMore = new Button("Show more");
            Button delete = new Button("Delete");
            ProductTm tm = new ProductTm(dto.getCode(), dto.getDescription(), showMore, delete);
            tms.add(tm);
        }
        tbl.setItems(tms);
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
            FXMLLoader fxmlLoader =
                    new FXMLLoader(getClass()
                            .getResource("../view/NewBatchForm.fxml"));
            Parent parent = fxmlLoader.load();
            NewBatchFormController controller = fxmlLoader.getController();
            controller.setDetails(Integer.parseInt(txtSelectedProdId.getText())
                    , txtSelectedProdDescription.getText(), stage, state, tm);
            stage.setScene(new Scene(parent));
            stage.show();
            stage.centerOnScreen();
        } else {
            new Alert(Alert.AlertType.WARNING, "Please select a valid one!");
        }
    }

    private void loadBatchData(int code) throws SQLException, ClassNotFoundException {
        ObservableList<ProductDetailTm> obList = FXCollections.observableArrayList();
        for (ProductDetailDto p : detailBo.findAllProductDetails(code)
        ) {
            Button btn = new Button("Delete");
            ProductDetailTm tm = new ProductDetailTm(
                    p.getCode(), p.getQtyOnHand(), p.getSellingPrice(),
                    p.getBuyingPrice(), p.isDiscountAvailability(),
                    p.getShowPrice(), btn
            );
            obList.add(tm);
        }
        tblDetail.setItems(obList);
    }

    private void setUi(String url) throws IOException {
        Stage stage = (Stage) context.getScene().getWindow();
        stage.setScene(
                new Scene(FXMLLoader.load(getClass().getResource("../view/" + url + ".fxml")))
        );
        stage.centerOnScreen();
    }
}
