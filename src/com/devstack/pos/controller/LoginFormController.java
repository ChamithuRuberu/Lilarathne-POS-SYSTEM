package com.devstack.pos.controller;

import com.devstack.pos.entity.AppUser;
import com.devstack.pos.service.SessionManager;
import com.devstack.pos.service.TrialService;
import com.devstack.pos.service.UserService;
import com.devstack.pos.util.JwtUtil;
import com.devstack.pos.util.StageManager;
import com.devstack.pos.util.UserSessionData;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class LoginFormController implements Initializable {
    public VBox context;
    public TextField txtEmail;
    public PasswordField txtPassword;

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final SessionManager sessionManager;
    private final TrialService trialService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up Enter key handler for email field - move to password field
        txtEmail.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtPassword.requestFocus();
            }
        });

        // Set up Enter key handler for password field - trigger login
        txtPassword.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                btnSignInOnAction(new ActionEvent());
            }
        });
    }

    public void btnCreateAnAccountOnAction(ActionEvent actionEvent) throws IOException {
        setUi("SignupForm");
    }

    public void btnSignInOnAction(ActionEvent actionEvent) {
        try {
            // Check trial version status before allowing login
            if (trialService.isTrialEnabled() && trialService.isTrialExpired()) {
                Alert trialAlert = new Alert(Alert.AlertType.ERROR);
                trialAlert.setTitle("Trial Period Expired");
                trialAlert.setHeaderText("Access Denied");
                trialAlert.setContentText("Your trial period has expired. Please contact the administrator to extend or activate the full version.\n\n" + 
                                         "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n" +
                                         "Status: " + trialService.getTrialStatusMessage());
                trialAlert.showAndWait();
                return;
            }
            
            AppUser appUser = userService.findUser(txtEmail.getText());
            if (appUser != null) {
                String jwtToken = userService.checkPassword(appUser.getEmail(), txtPassword.getText());
                if (!ObjectUtils.isEmpty(jwtToken)) {
                    // Store JWT token
                    UserSessionData.jwtToken = jwtToken;
                    UserSessionData.email = txtEmail.getText();
                    
                    // Extract and store user role from JWT token
                    String role = jwtUtil.getRoleFromToken(jwtToken);
                    UserSessionData.userRole = role != null ? role : "ROLE_CASHIER";
                    
                    // Initialize last activity time
                    UserSessionData.updateLastActivity();
                    
                    // Start session monitoring for inactivity and token expiration
                    sessionManager.startSessionMonitoring();
                    
                    System.out.println("=== LOGIN SUCCESSFUL ===");
                    System.out.println("User: " + txtEmail.getText());
                    System.out.println("Role from JWT: " + role);
                    System.out.println("Stored Role: " + UserSessionData.userRole);
                    System.out.println("Is Admin: " + UserSessionData.isAdmin());
                    System.out.println("Is Cashier: " + UserSessionData.isCashier());
                    System.out.println("Session monitoring started");
                    System.out.println("========================");
                    
                    // Show trial warning ONLY if 7 days or less remaining
                    if (trialService.isTrialActive() && trialService.isTrialWarningPeriod()) {
                        long daysRemaining = trialService.getDaysRemaining();
                        Alert trialWarning = new Alert(Alert.AlertType.WARNING);
                        trialWarning.setTitle("Trial Version Warning");
                        trialWarning.setHeaderText("⚠️Trial Period Ending Soon");
                        
                        String message;
                        if (daysRemaining == 0) {
                            message = "⚠️WARNING: Your trial period expires TODAY!\n\n" +
                                     "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n\n" +
                                     "Please contact the administrator to extend or activate the full version to avoid service interruption.";
                        } else if (daysRemaining == 1) {
                            message = "⚠️WARNING: Your trial period expires TOMORROW!\n\n" +
                                     "Days remaining: " + daysRemaining + " day\n" +
                                     "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n\n" +
                                     "Please contact the administrator to extend or activate the full version.";
                        } else {
                            message = "⚠️WARNING: Your trial period is ending soon!\n\n" +
                                     "Days remaining: " + daysRemaining + " days\n" +
                                     "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n\n" +
                                     "Please contact the administrator to extend or activate the full version before the trial expires.";
                        }
                        
                        trialWarning.setContentText(message);
                        trialWarning.show();
                    }
                    // No popup if more than 7 days remaining
                    
                    setUi("DashboardForm");
                } else {
                    new Alert(Alert.AlertType.WARNING, "check your password and try again!").show();
                }
            } else {
                new Alert(Alert.AlertType.WARNING, "User email not found!").show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    private void setUi(String url) throws IOException {
        Stage stage = (Stage) context.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/com/devstack/pos/view/" + url + ".fxml"));
        loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
        
        Parent root = loader.load();
        
        // Use auth screen size for SignupForm, full screen for DashboardForm
        if ("SignupForm".equals(url)) {
            StageManager.loadAuthScene(stage, root);
        } else {
            StageManager.loadFullScreenScene(stage, root);
    }
}
}