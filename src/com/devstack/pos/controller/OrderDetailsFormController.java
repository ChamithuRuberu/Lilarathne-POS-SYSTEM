package com.devstack.pos.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OrderDetailsFormController {
    public AnchorPane context;
    public TableColumn colId;
    public TableColumn colEmail;
    public TableColumn colDate;
    public TableColumn colDiscount;
    public TableColumn colTotal;
    public TextField txtEmailSearch;
    public TableColumn colCustomerEmail;
    public TableColumn colUserEmail;

    public void BackToHomeOnAction(ActionEvent actionEvent) throws IOException {
        setUi("DashboardForm");
    }

    public void OderDetailsShowOnAction(ActionEvent actionEvent) {
        // TODO: Implement order details display
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
