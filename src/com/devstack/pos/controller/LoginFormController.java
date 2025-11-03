package com.devstack.pos.controller;

import com.devstack.pos.entity.AppUser;
import com.devstack.pos.service.UserService;
import com.devstack.pos.util.UserSessionData;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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

    public void btnCreateAnAccountOnAction(ActionEvent actionEvent) throws IOException {
        setUi("SignupForm");
    }

    public void btnSignInOnAction(ActionEvent actionEvent) {
        try {
            AppUser appUser = userService.findUser(txtEmail.getText());
            if (appUser != null) {
                String checked = userService.checkPassword(appUser.getEmail(), txtPassword.getText());
                if (!ObjectUtils.isEmpty(checked)) {
                    UserSessionData.jwtToken = checked;
                    UserSessionData.email = txtEmail.getText();
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

        Scene scene = new Scene(loader.load());

        // Load CSS stylesheet
        try {
            var cssUrl = getClass().getResource("/com/devstack/pos/view/styles/pos-styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("Failed to load CSS: " + e.getMessage());
        }

        stage.setScene(scene);
        stage.centerOnScreen();
    }
}