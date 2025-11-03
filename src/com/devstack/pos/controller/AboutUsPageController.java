package com.devstack.pos.controller;

import com.devstack.pos.util.UserSessionData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import org.springframework.stereotype.Component;

@Component
public class AboutUsPageController extends BaseController {
    
    @FXML
    private Text txtUserEmail;
    

    
    @Override
    protected String getCurrentPageName() {
        return "About Us";
    }
}

