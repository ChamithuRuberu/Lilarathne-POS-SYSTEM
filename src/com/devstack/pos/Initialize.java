package com.devstack.pos;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

public class Initialize extends Application {

    private static Stage primaryStage;
    private static ConfigurableApplicationContext springContext;

    public static void setSpringContext(ConfigurableApplicationContext context) {
        springContext = context;
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Initialize.primaryStage = primaryStage;
        
        if (springContext == null) {
            throw new IllegalStateException("Spring context not initialized");
        }
        
        // Get Spring context
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/com/devstack/pos/view/LoginForm.fxml"));
        loader.setControllerFactory(springContext::getBean);
        
        primaryStage.setScene(new Scene(loader.load()));
        primaryStage.setTitle("POS System");
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
        System.exit(0);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
