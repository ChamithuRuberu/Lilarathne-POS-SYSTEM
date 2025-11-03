package com.devstack.pos.controller;

import com.devstack.pos.util.AuthorizationUtil;
import com.devstack.pos.util.StageManager;
import com.devstack.pos.util.UserSessionData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DashboardFormController {
    public AnchorPane context;
    
    @FXML
    public void initialize() {
        System.out.println("Dashboard loaded for user: " + UserSessionData.email + " with role: " + UserSessionData.userRole);
    }

    public void btnCustomerOnAction(ActionEvent actionEvent) throws IOException {
        setUi("CustomerForm");
    }

    public void btnProductOnActions(ActionEvent actionEvent) throws IOException {
        // Products management: ADMIN only
        if (!AuthorizationUtil.canAccessProducts()) {
            AuthorizationUtil.showAdminOnlyAlert();
            return;
        }
        setUi("ProductMainForm");
    }

    public void btnPlaceOrderOnAction(ActionEvent actionEvent) throws IOException {
        // POS Orders: ADMIN and CASHIER
        if (!AuthorizationUtil.canAccessPOSOrders()) {
            AuthorizationUtil.showUnauthorizedAlert();
            return;
        }
        setUi("PlaceOrderForm");
    }

    public void btnOrderDetailsOnAction(ActionEvent actionEvent) throws IOException {
        // Order Details: Accessible to all
        setUi("OrderDetailsForm");
    }

    public void btnIncomeReportOnAction(ActionEvent actionEvent) throws IOException {
        // Reports/Analysis: ADMIN only
        if (!AuthorizationUtil.canAccessReports()) {
            AuthorizationUtil.showAdminOnlyAlert();
            return;
        }
        setUi("AnalysisPage");
    }

    private void setUi(String url) throws IOException {
        Stage stage = (Stage) context.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/com/devstack/pos/view/" + url + ".fxml"));
        loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
        
        Parent root = loader.load();
        
        // All main screens should be full screen
        StageManager.loadFullScreenScene(stage, root);
    }

    public void btnDashboardOnAction(ActionEvent actionEvent) {
    }

    public void btnReturnsOrderOnAction(ActionEvent actionEvent) throws IOException {
        // Return Orders: ADMIN and CASHIER
        if (!AuthorizationUtil.canAccessReturnOrders()) {
            AuthorizationUtil.showUnauthorizedAlert();
            return;
        }
        setUi("ReturnOrdersForm");
    }

    public void btnPurchaseOnAction(ActionEvent actionEvent) throws IOException {
        // Purchase Orders: ADMIN only
        if (!AuthorizationUtil.canAccessPurchaseOrders()) {
            AuthorizationUtil.showAdminOnlyAlert();
            return;
        }
        setUi("PurchaseOrdersForm");
    }

    public void btnLogoutOnAction(ActionEvent actionEvent) {
        try {
            // Clear session data
            UserSessionData.clear();
            System.out.println("User logged out");
            
            // Return to login screen with auth screen size
            Stage stage = (Stage) context.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/devstack/pos/view/LoginForm.fxml"));
            loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
            
            Parent root = loader.load();
            StageManager.loadAuthScene(stage, root);
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to logout: " + e.getMessage()).show();
        }
    }

    public void btnPurchaseReturnOnAction(ActionEvent actionEvent) {
        // TODO: Implement purchase return functionality
        new Alert(Alert.AlertType.INFORMATION, "Purchase Return feature coming soon!").show();
    }

    public void btnStockValuationOnAction(ActionEvent actionEvent) {
        // Stock Valuation: ADMIN only
        if (!AuthorizationUtil.canAccessReports()) {
            AuthorizationUtil.showAdminOnlyAlert();
            return;
        }
        // TODO: Implement stock valuation functionality
        new Alert(Alert.AlertType.INFORMATION, "Stock Valuation feature coming soon!").show();
    }
}
