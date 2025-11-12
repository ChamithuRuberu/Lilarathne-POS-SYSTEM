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
    public void start(Stage primaryStage) {
        Initialize.primaryStage = primaryStage;

        try {
            if (springContext == null) {
                throw new IllegalStateException("Spring context not initialized");
            }

            FXMLLoader loader = new FXMLLoader();
            var fxmlUrl = getClass().getResource("/com/devstack/pos/view/LoginForm.fxml");

            if (fxmlUrl == null) {
                System.err.println("FXML file not found");
                throw new IOException("Could not find LoginForm.fxml");
            }

            loader.setLocation(fxmlUrl);
            loader.setControllerFactory(springContext::getBean);

            System.out.println("Loading FXML...");
            Scene scene = new Scene(loader.load());
            System.out.println("FXML loaded successfully");

            // Load CSS
            try {
                var cssUrl = getClass().getResource("/com/devstack/pos/view/styles/pos-styles.css");
                if (cssUrl != null) {
                    System.out.println("CSS found: " + cssUrl);
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                    System.out.println("CSS loaded successfully");
                } else {
                    System.err.println("CSS not found");
                }
            } catch (Exception cssEx) {
                System.err.println("════════════════════════════════════");
                System.err.println("CSS EXCEPTION:");
                cssEx.printStackTrace(System.err);
                System.err.println("════════════════════════════════════");
            }

            System.out.println("About to set scene...");
            primaryStage.setScene(scene);
            primaryStage.setTitle("POS System");
            primaryStage.setMinWidth(400);
            primaryStage.setMinHeight(500);

            System.out.println("About to show window...");
            primaryStage.show();
            primaryStage.toFront();

            System.out.println("JavaFX application started successfully!");

        } catch (Throwable e) {  // Changed to Throwable to catch everything
            System.err.println("════════════════════════════════════");
            System.err.println("FATAL ERROR:");
            e.printStackTrace(System.err);

            Throwable cause = e.getCause();
            int depth = 0;
            while (cause != null && depth < 10) {
                System.err.println("────────────────────────────────────");
                System.err.println("Caused by (" + depth + "):");
                cause.printStackTrace(System.err);
                cause = cause.getCause();
                depth++;
            }
            System.err.println("════════════════════════════════════");
            Platform.exit();
            System.exit(1);
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
