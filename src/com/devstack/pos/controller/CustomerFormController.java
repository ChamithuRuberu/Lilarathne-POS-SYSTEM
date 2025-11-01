package com.devstack.pos.controller;

import com.devstack.pos.entity.Customer;
import com.devstack.pos.service.CustomerService;
import com.devstack.pos.view.tm.CustomerTm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomerFormController {

    public AnchorPane context;
    public TextField txtEmail;
    public TextField txtName;
    public TextField txtContact;
    public TextField txtSalary;
    public Button btnSaveUpdate;
    public TextField txtSearch;
    public TableView<CustomerTm> tbl;
    public TableColumn colId;
    public TableColumn colEmail;
    public TableColumn colName;
    public TableColumn colContact;
    public TableColumn colSalary;
    public TableColumn colOperate;

    private String searchText = "";
    private final CustomerService customerService;

    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contact"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("salary"));
        colOperate.setCellValueFactory(new PropertyValueFactory<>("deleteButton"));

        loadAllCustomers(searchText);

        tbl.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        setData(newValue);
                    }
                });
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            searchText = newValue;
            loadAllCustomers(searchText);
        });
    }

    private void setData(CustomerTm newValue) {
        txtEmail.setEditable(false);
        btnSaveUpdate.setText("Update Customer");
        txtEmail.setText(newValue.getEmail());
        txtName.setText(newValue.getName());
        txtSalary.setText(String.valueOf(newValue.getSalary()));
        txtContact.setText(newValue.getContact());
    }

    private void loadAllCustomers(String searchText) {
        ObservableList<CustomerTm> observableList = FXCollections.observableArrayList();
        int counter = 1;
        for (Customer customer : (searchText.length() > 0 ? customerService.searchCustomers(searchText) : customerService.findAllCustomers())) {
            Button btn = new Button("Delete");
            CustomerTm tm = new CustomerTm(
                    counter, customer.getEmail(), customer.getName(), customer.getContact(), customer.getSalary(),
                    btn
            );
            observableList.add(tm);
            counter++;

            btn.setOnAction((e) -> {
                try {
                    Alert alert = new
                            Alert(Alert.AlertType.CONFIRMATION,
                            "Are you sure?", ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> selectedButtonType = alert.showAndWait();
                    if (selectedButtonType.isPresent() && selectedButtonType.get().equals(ButtonType.YES)) {
                        if (customerService.deleteCustomer(customer.getEmail())) {
                            new Alert(Alert.AlertType.CONFIRMATION, "Customer Deleted!").show();
                            loadAllCustomers(searchText);
                        } else {
                            new Alert(Alert.AlertType.WARNING, "Try Again!").show();
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, exception.getMessage()).show();
                }
            });
        }
        tbl.setItems(observableList);
    }

    public void btnSaveUpdateOnAction(ActionEvent actionEvent) {
        try {
            Customer customer = new Customer(txtEmail.getText(), txtName.getText(),
                    txtContact.getText(), Double.parseDouble(txtSalary.getText()));

            if (btnSaveUpdate.getText().equals("Save Customer")) {
                if (customerService.saveCustomer(customer)) {
                    new Alert(Alert.AlertType.CONFIRMATION, "Customer Saved!").show();
                    clearFields();
                    loadAllCustomers(searchText);
                } else {
                    new Alert(Alert.AlertType.WARNING, "Customer already exists or Try Again!").show();
                }
            } else {
                if (customerService.updateCustomer(customer)) {
                    new Alert(Alert.AlertType.CONFIRMATION, "Customer Updated!").show();
                    clearFields();
                    loadAllCustomers(searchText);
                    txtEmail.setEditable(true);
                    btnSaveUpdate.setText("Save Customer");
                } else {
                    new Alert(Alert.AlertType.WARNING, "Try Again!").show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    private void clearFields() {
        txtEmail.clear();
        txtName.clear();
        txtContact.clear();
        txtSalary.clear();
    }

    private void setUi(String url) throws IOException {
        Stage stage = (Stage) context.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/com/devstack/pos/view/" + url + ".fxml"));
        loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
        stage.setScene(new Scene(loader.load()));
        stage.centerOnScreen();
    }

    public void btnBackToHomeOnAction(ActionEvent actionEvent) throws IOException {
        setUi("DashboardForm");
    }

    public void btnNewCustomerOnAction(ActionEvent actionEvent) {
        txtEmail.setEditable(true);
        btnSaveUpdate.setText("Save Customer");
        clearFields();
    }

    public void searchCustomer(ActionEvent actionEvent) {

    }
}
