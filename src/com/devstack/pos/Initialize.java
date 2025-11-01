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

        try {
            // Get Spring context
            FXMLLoader loader = new FXMLLoader();

            // Try to find the FXML file
            var fxmlUrl = getClass().getResource("/view/LoginForm.fxml");

            if (fxmlUrl == null) {
                System.err.println("FXML file not found at: /view/LoginForm.fxml");
                System.err.println("Trying alternative paths...");

                // Try alternative path
                fxmlUrl = getClass().getResource("/view/LoginForm.fxml");
                if (fxmlUrl == null) {
                    throw new IOException("Could not find LoginForm.fxml in any expected location");
                }
                System.out.println("Found FXML at: /view/LoginForm.fxml");
            } else {
                System.out.println("Found FXML at: /view/LoginForm.fxml");
            }

            loader.setLocation(fxmlUrl);
            loader.setControllerFactory(springContext::getBean);

            primaryStage.setScene(new Scene(loader.load()));
            primaryStage.setTitle("POS System");
            primaryStage.centerOnScreen();
            primaryStage.show();

            System.out.println("JavaFX application started successfully!");

        } catch (Exception e) {
            System.err.println("Error starting JavaFX application:");
            e.printStackTrace();
            throw e;
        }
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
