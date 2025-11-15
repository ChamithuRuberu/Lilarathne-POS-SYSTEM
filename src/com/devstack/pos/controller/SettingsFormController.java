package com.devstack.pos.controller;

import com.devstack.pos.entity.SystemSettings;
import com.devstack.pos.service.SystemSettingsService;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsFormController extends BaseController {
    
    private final SystemSettingsService systemSettingsService;
    
    @FXML
    public AnchorPane context;
    
    @FXML
    public Text txtUserEmail;
    
    @FXML
    public Text txtUserRole;
    
    @FXML
    public JFXButton btnDashboard;
    
    @FXML
    public JFXButton btnCustomer;
    
    @FXML
    public JFXButton btnProduct;
    
    @FXML
    public JFXButton btnPlaceOrder;
    
    @FXML
    public JFXButton btnPendingPayments;
    
    @FXML
    public JFXButton btnReturns;
    
    @FXML
    public JFXButton btnPurchase;
    
    @FXML
    public JFXButton btnOrderDetails;
    
    @FXML
    public JFXButton btnReports;
    
    @FXML
    public JFXButton btnHelp;
    
    @FXML
    public JFXButton btnAboutUs;
    
    @FXML
    private JFXTextField txtBusinessName;
    
    @FXML
    private TextArea txtAddress;
    
    @FXML
    private JFXTextField txtContactNumber;
    
    @FXML
    private JFXTextField txtEmail;
    
    @FXML
    private JFXTextField txtTaxNumber;
    
    @FXML
    private TextArea txtFooterMessage;
    
    @FXML
    public void initialize() {
        initializeSidebar();
        loadSystemSettings();
    }
    
    @Override
    protected String getCurrentPageName() {
        return "Settings";
    }
    
    private void loadSystemSettings() {
        try {
            SystemSettings settings = systemSettingsService.getSystemSettings();
            if (settings != null) {
                txtBusinessName.setText(settings.getBusinessName() != null ? settings.getBusinessName() : "");
                txtAddress.setText(settings.getAddress() != null ? settings.getAddress() : "");
                txtContactNumber.setText(settings.getContactNumber() != null ? settings.getContactNumber() : "");
                txtEmail.setText(settings.getEmail() != null ? settings.getEmail() : "");
                txtTaxNumber.setText(settings.getTaxNumber() != null ? settings.getTaxNumber() : "");
                txtFooterMessage.setText(settings.getFooterMessage() != null ? settings.getFooterMessage() : "");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Failed to load system settings: " + e.getMessage());
        }
    }
    
    @FXML
    public void btnSaveOnAction(ActionEvent event) {
        try {
            // Validate required fields
            if (txtBusinessName.getText().trim().isEmpty()) {
                showWarning("Validation Error", "Business name is required.");
                return;
            }
            
            SystemSettings settings = new SystemSettings();
            settings.setBusinessName(txtBusinessName.getText().trim());
            settings.setAddress(txtAddress.getText().trim());
            settings.setContactNumber(txtContactNumber.getText().trim());
            settings.setEmail(txtEmail.getText().trim());
            settings.setTaxNumber(txtTaxNumber.getText().trim());
            settings.setFooterMessage(txtFooterMessage.getText().trim());
            
            systemSettingsService.updateSystemSettings(settings);
            showSuccess("Success", "System settings saved successfully!");
            
            // Optionally navigate back to dashboard
            // navigateTo("DashboardForm", false);
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "Failed to save system settings: " + e.getMessage());
        }
    }
    
    @FXML
    public void btnCancelOnAction(ActionEvent event) {
        navigateTo("DashboardForm", false);
    }
}

