package com.devstack.pos.controller;

import com.devstack.pos.entity.AppUser;
import com.devstack.pos.service.SessionManager;
import com.devstack.pos.service.UserService;
import com.devstack.pos.util.JwtUtil;
import com.devstack.pos.util.StageManager;
import com.devstack.pos.util.UserSessionData;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginFormController {
    public VBox context;
    public TextField txtEmail;
    public PasswordField txtPassword;

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final SessionManager sessionManager;

    public void btnCreateAnAccountOnAction(ActionEvent actionEvent) throws IOException {
        setUi("SignupForm");
    }

    public void btnSignInOnAction(ActionEvent actionEvent) {
        try {
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