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
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Base controller class with common navigation and UI functionality
 * All main screen controllers should extend this class for consistent behavior
 */
public abstract class BaseController {
    
    @FXML
    public AnchorPane context;
    
    @FXML
    protected Text txtUserEmail;
    
    @FXML
    protected Text txtUserRole;
    
    /**
     * Initialize user info in sidebar
     * Call this from your controller's initialize() method
     */
    protected void initializeSidebar() {
        if (txtUserEmail != null) {
            txtUserEmail.setText(UserSessionData.email);
        }
        if (txtUserRole != null) {
            String role = UserSessionData.userRole;
            // Format role for display (remove ROLE_ prefix if present)
            String displayRole = role.replace("ROLE_", "");
            txtUserRole.setText("ROLE: " + displayRole);
        }
    }
    
    /**
     * Get the name of the current page for highlighting active menu item
     * Override this in child controllers
     */
    protected abstract String getCurrentPageName();
    
    // ===== COMMON NAVIGATION METHODS =====
    
    @FXML
    public void btnDashboardOnAction(ActionEvent event) {
        navigateTo("DashboardForm", true);
    }
    
    @FXML
    public void btnCustomerOnAction(ActionEvent event) {
        if (AuthorizationUtil.canAccessCustomers()) {
            navigateTo("CustomerForm", true);
        } else {
            AuthorizationUtil.showUnauthorizedAlert();
        }
    }
    
    @FXML
    public void btnProductOnAction(ActionEvent event) {
        if (AuthorizationUtil.canAccessProducts()) {
            navigateTo("ProductMainForm", true);
        } else {
            AuthorizationUtil.showAdminOnlyAlert();
        }
    }
    
    @FXML
    public void btnPlaceOrderOnAction(ActionEvent event) {
        if (AuthorizationUtil.canAccessPOSOrders()) {
            navigateTo("PlaceOrderForm", true);
        } else {
            AuthorizationUtil.showUnauthorizedAlert();
        }
    }
    
    @FXML
    public void btnPendingPaymentsOnAction(ActionEvent event) {
        if (AuthorizationUtil.canAccessPOSOrders()) {
            navigateTo("PendingPaymentsForm", true);
        } else {
            AuthorizationUtil.showUnauthorizedAlert();
        }
    }
    
    @FXML
    public void btnOrderDetailsOnAction(ActionEvent event) {
        navigateTo("OrderDetailsForm", true);
    }
    
    @FXML
    public void btnReturnsOnAction(ActionEvent event) {
        if (AuthorizationUtil.canAccessReturnOrders()) {
            navigateTo("ReturnOrdersForm", true);
        } else {
            AuthorizationUtil.showUnauthorizedAlert();
        }
    }
    
    @FXML
    public void btnPurchaseOnAction(ActionEvent event) {
        if (AuthorizationUtil.canAccessPurchaseOrders()) {
            navigateTo("SupplierManagementForm", true);
        } else {
            AuthorizationUtil.showAdminOnlyAlert();
        }
    }
    
    @FXML
    public void btnReportsOnAction(ActionEvent event) {
        if (AuthorizationUtil.canAccessReports()) {
            navigateTo("AnalysisPage", true);
        } else {
            AuthorizationUtil.showAdminOnlyAlert();
        }
    }
    
    @FXML
    public void btnHelpOnAction(ActionEvent event) {
        navigateTo("HelpPage", false);
    }
    
    @FXML
    public void btnAboutUsOnAction(ActionEvent event) {
        navigateTo("AboutUsPage", false);
    }
    
    @FXML
    public void btnLogoutOnAction(ActionEvent event) {
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
    
    // ===== HELPER METHODS =====
    
    /**
     * Navigate to another screen
     * @param viewName FXML file name without extension
     * @param checkAuth Whether to check authorization
     */
    protected void navigateTo(String viewName, boolean checkAuth) {
        try {
            Stage stage = (Stage) context.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/com/devstack/pos/view/" + viewName + ".fxml"));
            loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
            
            Parent root = loader.load();
            StageManager.loadFullScreenScene(stage, root);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Navigation Error", "Failed to navigate to " + viewName + ": " + e.getMessage());
        }
    }
    
    /**
     * Show error alert
     */
    protected void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show success alert
     */
    protected void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show warning alert
     */
    protected void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

