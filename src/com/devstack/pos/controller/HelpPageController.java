package com.devstack.pos.controller;

import com.devstack.pos.util.UserSessionData;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import org.springframework.stereotype.Component;

@Component
public class HelpPageController extends BaseController {
    
    @FXML
    private Text txtUserEmail;
    
    @FXML
    public void initialize() {
        // Display user info in header
        if (txtUserEmail != null) {
            String role = UserSessionData.userRole.replace("ROLE_", "");
            txtUserEmail.setText("Welcome back, " + UserSessionData.email + " (" + role + ")");
        }
    }
    
    @Override
    protected String getCurrentPageName() {
        return "Help";
    }
}

