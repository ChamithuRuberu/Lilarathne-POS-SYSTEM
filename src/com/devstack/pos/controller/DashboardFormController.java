package com.devstack.pos.controller;

import com.devstack.pos.util.UserSessionData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.springframework.stereotype.Component;

@Component
public class DashboardFormController extends BaseController {
    
    @FXML
    public void initialize() {
        // Initialize sidebar with user info
        initializeSidebar();
        
        System.out.println("Dashboard loaded for user: " + UserSessionData.email + " with role: " + UserSessionData.userRole);
        
        // Load any dashboard-specific data here
    }
    
    @Override
    protected String getCurrentPageName() {
        return "Dashboard";
    }
    
    // Dashboard-specific action handlers
    
    // Alias for FXML compatibility (DashboardForm.fxml uses btnProductOnActions with 's')
    @FXML
    public void btnProductOnActions(ActionEvent actionEvent) {
        btnProductOnAction(actionEvent);
    }
    
    // Alias for FXML compatibility (DashboardForm.fxml uses btnReturnsOrderOnAction)
    @FXML
    public void btnReturnsOrderOnAction(ActionEvent actionEvent) {
        btnReturnsOnAction(actionEvent);
    }
    
    // Alias for FXML compatibility (DashboardForm.fxml uses btnIncomeReportOnAction)
    @FXML
    public void btnIncomeReportOnAction(ActionEvent actionEvent) {
        btnReportsOnAction(actionEvent);
    }
    
    @FXML
    public void btnPurchaseReturnOnAction(ActionEvent actionEvent) {
        // TODO: Implement purchase return functionality
        showWarning("Coming Soon", "Purchase Return feature coming soon!");
    }

    @FXML
    public void btnStockValuationOnAction(ActionEvent actionEvent) {
        // Stock Valuation: ADMIN only
        if (!com.devstack.pos.util.AuthorizationUtil.canAccessReports()) {
            com.devstack.pos.util.AuthorizationUtil.showAdminOnlyAlert();
            return;
        }
        // TODO: Implement stock valuation functionality
        showWarning("Coming Soon", "Stock Valuation feature coming soon!");
    }
}
