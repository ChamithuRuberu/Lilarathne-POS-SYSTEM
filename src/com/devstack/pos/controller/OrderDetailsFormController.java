package com.devstack.pos.controller;

import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.view.tm.OrderTm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderDetailsFormController {
    public AnchorPane context;
    public TableColumn colId;
    public TableColumn colCustomerEmail;
    public TableColumn colDate;
    public TableColumn colDiscount;
    public TableColumn colUserEmail;
    public TableColumn colTotal;
    public TextField txtEmailSearch;
    public TableView<OrderTm> tblOrders;

    private final OrderDetailService orderDetailService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void initialize() {
        // Configure table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("code"));
        colCustomerEmail.setCellValueFactory(new PropertyValueFactory<>("customerEmail"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("issuedDate"));
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("discount"));
        colUserEmail.setCellValueFactory(new PropertyValueFactory<>("operatorEmail"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        
        // Format date column
        colDate.setCellFactory(column -> new javafx.scene.control.TableCell<OrderTm, java.time.LocalDateTime>() {
            @Override
            protected void updateItem(java.time.LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(dateTimeFormatter));
                }
            }
        });
        
        // Format numeric columns
        colDiscount.setCellFactory(column -> new javafx.scene.control.TableCell<OrderTm, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                }
            }
        });
        
        colTotal.setCellFactory(column -> new javafx.scene.control.TableCell<OrderTm, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f /=", item));
                }
            }
        });
        
        // Load all orders initially
        loadAllOrders();
        
        // Add search functionality - load orders when text changes
        txtEmailSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                loadAllOrders();
            }
        });
    }

    public void BackToHomeOnAction(ActionEvent actionEvent) throws IOException {
        setUi("DashboardForm");
    }

    public void OderDetailsShowOnAction(ActionEvent actionEvent) {
        String searchEmail = txtEmailSearch.getText().trim();
        
        if (searchEmail.isEmpty()) {
            loadAllOrders();
        } else {
            loadOrdersByCustomerEmail(searchEmail);
        }
    }
    
    private void loadAllOrders() {
        List<OrderDetail> orderDetails = orderDetailService.findAllOrderDetails();
        displayOrders(orderDetails);
    }
    
    private void loadOrdersByCustomerEmail(String email) {
        List<OrderDetail> orderDetails = orderDetailService.findByCustomerEmail(email);
        displayOrders(orderDetails);
        
        if (orderDetails.isEmpty()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION,
                    "No orders found for customer: " + email
            );
            alert.show();
        }
    }
    
    private void displayOrders(List<OrderDetail> orderDetails) {
        ObservableList<OrderTm> observableList = FXCollections.observableArrayList();
        
        for (OrderDetail orderDetail : orderDetails) {
            OrderTm tm = new OrderTm(
                    orderDetail.getCode(),
                    orderDetail.getCustomerEmail(),
                    orderDetail.getIssuedDate(),
                    orderDetail.getDiscount(),
                    orderDetail.getOperatorEmail(),
                    orderDetail.getTotalCost()
            );
            observableList.add(tm);
        }
        
        tblOrders.setItems(observableList);
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
