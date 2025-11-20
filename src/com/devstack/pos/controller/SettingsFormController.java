package com.devstack.pos.controller;

import com.devstack.pos.entity.SystemSettings;
import com.devstack.pos.service.SystemSettingsService;
import com.devstack.pos.service.TrialService;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SettingsFormController extends BaseController {
    
    private final SystemSettingsService systemSettingsService;
    private final TrialService trialService;
    
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
    private CheckBox chkTrialEnabled;
    
    @FXML
    private DatePicker datePickerTrialEndDate;
    
    @FXML
    private Text txtTrialStatus;
    
    @FXML
    public void initialize() {
        initializeSidebar();
        loadSystemSettings();
        setupTrialControls();
    }
    
    private void setupTrialControls() {
        // Enable/disable date picker based on checkbox
        chkTrialEnabled.setOnAction(e -> {
            datePickerTrialEndDate.setDisable(!chkTrialEnabled.isSelected());
            if (!chkTrialEnabled.isSelected()) {
                datePickerTrialEndDate.setValue(null);
            }
        });
        
        // Update trial status display
        updateTrialStatus();
    }
    
    private void updateTrialStatus() {
        try {
            String status = trialService.getTrialStatusMessage();
            txtTrialStatus.setText("Status: " + status);
            
            // Set color based on status
            if (trialService.isTrialExpired()) {
                txtTrialStatus.setStyle("-fx-fill: #ef4444; -fx-font-weight: bold;");
            } else if (trialService.isTrialActive()) {
                txtTrialStatus.setStyle("-fx-fill: #f59e0b; -fx-font-weight: bold;");
            } else {
                txtTrialStatus.setStyle("-fx-fill: #64748b;");
            }
        } catch (Exception e) {
            txtTrialStatus.setText("Status: Unable to load");
            txtTrialStatus.setStyle("-fx-fill: #64748b;");
        }
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
                
                // Load trial settings
                chkTrialEnabled.setSelected(settings.getTrialEnabled() != null && settings.getTrialEnabled());
                datePickerTrialEndDate.setValue(settings.getTrialEndDate());
                datePickerTrialEndDate.setDisable(!chkTrialEnabled.isSelected());
            }
            updateTrialStatus();
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
            
            // Validate trial settings
            if (chkTrialEnabled.isSelected() && datePickerTrialEndDate.getValue() == null) {
                showWarning("Validation Error", "Please select a trial end date when trial is enabled.");
                return;
            }
            
            if (chkTrialEnabled.isSelected() && datePickerTrialEndDate.getValue() != null) {
                LocalDate endDate = datePickerTrialEndDate.getValue();
                if (endDate.isBefore(LocalDate.now())) {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Confirm Past Date");
                    confirmAlert.setHeaderText("Trial End Date is in the Past");
                    confirmAlert.setContentText("The selected trial end date is in the past. This will immediately expire the trial.\n\nDo you want to continue?");
                    var result = confirmAlert.showAndWait();
                    if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
                        return; // User cancelled
                    }
                }
            }
            
            SystemSettings settings = new SystemSettings();
            settings.setBusinessName(txtBusinessName.getText().trim());
            settings.setAddress(txtAddress.getText().trim());
            settings.setContactNumber(txtContactNumber.getText().trim());
            settings.setEmail(txtEmail.getText().trim());
            settings.setTaxNumber(txtTaxNumber.getText().trim());
            settings.setFooterMessage(txtFooterMessage.getText().trim());
            
            // Set trial settings
            settings.setTrialEnabled(chkTrialEnabled.isSelected());
            settings.setTrialEndDate(chkTrialEnabled.isSelected() ? datePickerTrialEndDate.getValue() : null);
            
            systemSettingsService.updateSystemSettings(settings);
            showSuccess("Success", "System settings saved successfully!");
            
            // Update trial status display
            updateTrialStatus();
            
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

