package com.devstack.pos.controller;

import com.devstack.pos.util.AuthorizationUtil;
import com.devstack.pos.util.UserSessionData;
import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import org.springframework.stereotype.Component;

@Component
public class HelpPageController extends BaseController {
    
    @FXML
    private Text txtUserEmail;
    
    // Menu buttons for role-based visibility
    @FXML
    private JFXButton btnDashboard;
    
    @FXML
    private JFXButton btnCustomer;
    
    @FXML
    private JFXButton btnProduct;
    
    @FXML
    private JFXButton btnPlaceOrder;
    
    @FXML
    private JFXButton btnReturns;
    
    @FXML
    private JFXButton btnPurchase;
    
    @FXML
    private JFXButton btnOrderDetails;
    
    @FXML
    private JFXButton btnReports;
    
    @FXML
    private JFXButton btnHelp;
    
    @FXML
    private JFXButton btnAboutUs;
    
    @FXML
    private JFXButton btnLogout;
    
    @FXML
    public void initialize() {
        // Initialize sidebar with user info
        initializeSidebar();
        
        // Configure menu visibility based on user role
        configureMenuVisibility();
    }
    
    /**
     * Configure menu visibility based on user role
     * Normal users: Dashboard, POS/Orders, Return Orders, All Orders, Help, About Us
     * Admin users: All features
     */
    private void configureMenuVisibility() {
        boolean isAdmin = AuthorizationUtil.isAdmin();
        
        // Always visible for all users
        if (btnDashboard != null) {
            btnDashboard.setVisible(true);
            btnDashboard.setManaged(true);
        }
        if (btnPlaceOrder != null) {
            btnPlaceOrder.setVisible(true);
            btnPlaceOrder.setManaged(true);
        }
        if (btnReturns != null) {
            btnReturns.setVisible(true);
            btnReturns.setManaged(true);
        }
        if (btnOrderDetails != null) {
            btnOrderDetails.setVisible(true);
            btnOrderDetails.setManaged(true);
        }
        if (btnHelp != null) {
            btnHelp.setVisible(true);
            btnHelp.setManaged(true);
        }
        if (btnAboutUs != null) {
            btnAboutUs.setVisible(true);
            btnAboutUs.setManaged(true);
        }
        
        // Admin-only features
        if (btnCustomer != null) {
            btnCustomer.setVisible(isAdmin);
            btnCustomer.setManaged(isAdmin);
        }
        if (btnProduct != null) {
            btnProduct.setVisible(isAdmin);
            btnProduct.setManaged(isAdmin);
        }
        if (btnPurchase != null) {
            btnPurchase.setVisible(isAdmin);
            btnPurchase.setManaged(isAdmin);
        }
        if (btnReports != null) {
            btnReports.setVisible(isAdmin);
            btnReports.setManaged(isAdmin);
        }
    }
    
    @Override
    protected String getCurrentPageName() {
        return "Help";
    }
}

