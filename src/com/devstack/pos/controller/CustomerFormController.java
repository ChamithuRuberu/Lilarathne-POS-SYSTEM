package com.devstack.pos.controller;

import com.devstack.pos.entity.Customer;
import com.devstack.pos.service.CustomerService;
import com.devstack.pos.service.OrderDetailService;
import com.devstack.pos.view.tm.CustomerTm;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.devstack.pos.util.StageManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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
public class CustomerFormController extends BaseController {
    public TextField txtName;
    public TextField txtContact;
    public Button btnSaveUpdate;
    public TextField txtSearch;
    public TableView<CustomerTm> tbl;
    public TableColumn colId;
    public TableColumn colName;
    public TableColumn colContact;
    public TableColumn colTotalSpent;
    public TableColumn colPendingPayments;
    public TableColumn colOperate;

    private String searchText = "";
    private Long selectedCustomerId = null;
    private final CustomerService customerService;
    private final OrderDetailService orderDetailService;

    public void initialize() {
        // Initialize sidebar
        initializeSidebar();
        
        // Initialize table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contact"));
        colTotalSpent.setCellValueFactory(new PropertyValueFactory<>("totalSpent"));
        colPendingPayments.setCellValueFactory(new PropertyValueFactory<>("pendingPayments"));
        colOperate.setCellValueFactory(new PropertyValueFactory<>("deleteButton"));
        
        // Format total spent column to show currency
        colTotalSpent.setCellFactory(column -> new javafx.scene.control.TableCell<CustomerTm, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("Rs. %.2f", item));
                }
            }
        });
        
        // Format pending payments column to show currency
        colPendingPayments.setCellFactory(column -> new javafx.scene.control.TableCell<CustomerTm, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("Rs. %.2f", item));
                }
            }
        });

        // Load customers
        loadAllCustomers(searchText);

        // Table selection listener
        tbl.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        setData(newValue);
                    }
                });
                
        // Search listener
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            searchText = newValue;
            loadAllCustomers(searchText);
        });
    }
    
    @Override
    protected String getCurrentPageName() {
        return "Customers";
    }

    private void setData(CustomerTm newValue) {
        selectedCustomerId = newValue.getId();
        btnSaveUpdate.setText("Update Customer");
        txtName.setText(newValue.getName());
        txtContact.setText(newValue.getContact());
    }

    public void searchCustomer(ActionEvent actionEvent) {
        searchText = txtSearch.getText();
        loadAllCustomers(searchText);
    }

    private void loadAllCustomers(String searchText) {
        ObservableList<CustomerTm> observableList = FXCollections.observableArrayList();
        for (Customer customer : (searchText.length() > 0 ? customerService.searchCustomers(searchText) : customerService.findAllCustomers())) {
            // Get total spent by this customer using optimized query
            double totalSpent = orderDetailService.getTotalSpentByCustomerId(customer.getId());
            
            // Get pending payments total by this customer
            double pendingPayments = orderDetailService.getPendingPaymentsTotalByCustomerId(customer.getId());
            
            Button btn = new Button("Delete");
            CustomerTm tm = new CustomerTm(
                    customer.getId(), customer.getName(), customer.getContact(), totalSpent, pendingPayments, btn
            );
            observableList.add(tm);

            btn.setOnAction((e) -> {
                try {
                    Alert alert = new
                            Alert(Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to delete this customer?", ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> selectedButtonType = alert.showAndWait();
                    if (selectedButtonType.isPresent() && selectedButtonType.get().equals(ButtonType.YES)) {
                        if (customerService.deleteCustomer(customer.getId())) {
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
            // Validate inputs
            if (txtName.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please enter customer name!").show();
                return;
            }
            
            if (txtContact.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please enter contact number!").show();
                return;
            }

            if (btnSaveUpdate.getText().equals("Save Customer")) {
                Customer customer = new Customer(txtName.getText().trim(), txtContact.getText().trim());
                if (customerService.saveCustomer(customer)) {
                    new Alert(Alert.AlertType.CONFIRMATION, "Customer Saved Successfully!").show();
                    clearFields();
                    loadAllCustomers(searchText);
                } else {
                    new Alert(Alert.AlertType.WARNING, "Failed to save customer. Please try again!").show();
                }
            } else {
                if (selectedCustomerId != null) {
                    Customer customer = new Customer(selectedCustomerId, txtName.getText().trim(), txtContact.getText().trim());
                    if (customerService.updateCustomer(customer)) {
                        new Alert(Alert.AlertType.CONFIRMATION, "Customer Updated Successfully!").show();
                        clearFields();
                        loadAllCustomers(searchText);
                        selectedCustomerId = null;
                        btnSaveUpdate.setText("Save Customer");
                    } else {
                        new Alert(Alert.AlertType.WARNING, "Failed to update customer. Please try again!").show();
                    }
                } else {
                    new Alert(Alert.AlertType.WARNING, "No customer selected for update!").show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).show();
        }
    }

    private void clearFields() {
        txtName.clear();
        txtContact.clear();
        selectedCustomerId = null;
    }

    // Navigation methods are inherited from BaseController

    public void btnBackToHomeOnAction(ActionEvent actionEvent) {
        btnDashboardOnAction(actionEvent);
    }

    public void btnNewCustomerOnAction(ActionEvent actionEvent) {
        btnSaveUpdate.setText("Save Customer");
        clearFields();
        tbl.getSelectionModel().clearSelection();
    }
}
