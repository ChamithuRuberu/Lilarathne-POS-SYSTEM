package com.devstack.pos.controller;

import com.devstack.pos.service.SessionManager;
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
import lombok.Getter;
import lombok.Setter;

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
    
    @Getter
    @Setter
    private SessionManager sessionManager;
    
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
        
        // Setup activity tracking for mouse and keyboard events
        setupActivityTracking();
    }
    
    /**
     * Setup activity tracking for user interactions
     */
    private void setupActivityTracking() {
        if (context != null) {
            // Track mouse movements and clicks
            context.setOnMouseMoved(this::onUserActivity);
            context.setOnMouseClicked(this::onUserActivity);
            context.setOnMousePressed(this::onUserActivity);
            
            // Track keyboard events
            context.setOnKeyPressed(this::onUserActivity);
            context.setOnKeyTyped(this::onUserActivity);
        }
    }
    
    /**
     * Handle user activity - update last activity time
     */
    private void onUserActivity(javafx.event.Event event) {
        updateActivity();
    }
    
    /**
     * Update user activity timestamp
     * Can be called manually from child controllers if needed
     */
    protected void updateActivity() {
        UserSessionData.updateLastActivity();
        if (sessionManager != null) {
            sessionManager.updateActivity();
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
        updateActivity();
        navigateTo("DashboardForm", true);
    }
    
    @FXML
    public void btnCustomerOnAction(ActionEvent event) {
        updateActivity();
        if (AuthorizationUtil.canAccessCustomers()) {
            navigateTo("CustomerForm", true);
        } else {
            AuthorizationUtil.showUnauthorizedAlert();
        }
    }
    
    @FXML
    public void btnProductOnAction(ActionEvent event) {
        updateActivity();
        if (AuthorizationUtil.canAccessProducts()) {
            navigateTo("ProductMainForm", true);
        } else {
            AuthorizationUtil.showAdminOnlyAlert();
        }
    }
    
    @FXML
    public void btnPlaceOrderOnAction(ActionEvent event) {
        updateActivity();
        if (AuthorizationUtil.canAccessPOSOrders()) {
            navigateTo("PlaceOrderForm", true);
        } else {
            AuthorizationUtil.showUnauthorizedAlert();
        }
    }
    
    @FXML
    public void btnPendingPaymentsOnAction(ActionEvent event) {
        updateActivity();
        if (AuthorizationUtil.canAccessPOSOrders()) {
            navigateTo("PendingPaymentsForm", true);
        } else {
            AuthorizationUtil.showUnauthorizedAlert();
        }
    }
    
    @FXML
    public void btnOrderDetailsOnAction(ActionEvent event) {
        updateActivity();
        navigateTo("OrderDetailsForm", true);
    }
    
    @FXML
    public void btnReturnsOnAction(ActionEvent event) {
        updateActivity();
        if (AuthorizationUtil.canAccessReturnOrders()) {
            navigateTo("ReturnOrdersForm", true);
        } else {
            AuthorizationUtil.showUnauthorizedAlert();
        }
    }
    
    @FXML
    public void btnPurchaseOnAction(ActionEvent event) {
        updateActivity();
        if (AuthorizationUtil.canAccessPurchaseOrders()) {
            navigateTo("SupplierManagementForm", true);
        } else {
            AuthorizationUtil.showAdminOnlyAlert();
        }
    }
    
    @FXML
    public void btnReportsOnAction(ActionEvent event) {
        updateActivity();
        if (AuthorizationUtil.canAccessReports()) {
            navigateTo("AnalysisPage", true);
        } else {
            AuthorizationUtil.showAdminOnlyAlert();
        }
    }
    
    @FXML
    public void btnHelpOnAction(ActionEvent event) {
        updateActivity();
        navigateTo("HelpPage", false);
    }
    
    @FXML
    public void btnAboutUsOnAction(ActionEvent event) {
        updateActivity();
        navigateTo("AboutUsPage", false);
    }
    
    @FXML
    public void btnSettingsOnAction(ActionEvent event) {
        updateActivity();
        navigateTo("SettingsForm", false);
    }
    
    @FXML
    public void btnLogoutOnAction(ActionEvent event) {
        try {
            // Stop session monitoring
            if (sessionManager != null) {
                sessionManager.stopSessionMonitoring();
            }
            
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
            updateActivity(); // Track navigation as activity
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

