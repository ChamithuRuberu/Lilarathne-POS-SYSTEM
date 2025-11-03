package com.devstack.pos.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Utility class for managing stage/window configurations
 * Provides consistent UI sizing across the application
 */
public class StageManager {
    
    // Login and Signup window size
    private static final double AUTH_WINDOW_WIDTH = 600;
    private static final double AUTH_WINDOW_HEIGHT = 700;
    
    /**
     * Set scene to full screen (for main application screens)
     */
    public static void setFullScreen(Stage stage, Scene scene) {
        // Get screen dimensions
        Screen screen = Screen.getPrimary();
        double width = screen.getVisualBounds().getWidth();
        double height = screen.getVisualBounds().getHeight();
        
        // Set stage properties
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(0);
        stage.setY(0);
        
        // Optional: Set title
        if (stage.getTitle() == null || stage.getTitle().isEmpty()) {
            stage.setTitle("Lilarathne POS System");
        }
    }
    
    /**
     * Set scene to auth screen size (for login/signup)
     */
    public static void setAuthScreenSize(Stage stage, Scene scene) {
        stage.setScene(scene);
        stage.setWidth(AUTH_WINDOW_WIDTH);
        stage.setHeight(AUTH_WINDOW_HEIGHT);
        stage.setResizable(false);
        stage.centerOnScreen();
        
        if (stage.getTitle() == null || stage.getTitle().isEmpty()) {
            stage.setTitle("Lilarathne POS System - Authentication");
        }
    }
    
    /**
     * Load scene with full screen
     */
    public static void loadFullScreenScene(Stage stage, Parent root) {
        Scene scene = new Scene(root);
        loadCSS(scene);
        setFullScreen(stage, scene);
    }
    
    /**
     * Load scene with auth screen size
     */
    public static void loadAuthScene(Stage stage, Parent root) {
        Scene scene = new Scene(root);
        loadCSS(scene);
        setAuthScreenSize(stage, scene);
    }
    
    /**
     * Load CSS stylesheet into scene
     */
    private static void loadCSS(Scene scene) {
        try {
            var cssUrl = StageManager.class.getResource("/com/devstack/pos/view/styles/pos-styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("Failed to load CSS: " + e.getMessage());
        }
    }
    
    /**
     * Set dialog window size (for popup dialogs)
     */
    public static void setDialogSize(Stage stage, Scene scene, double width, double height) {
        stage.setScene(scene);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setResizable(false);
        stage.centerOnScreen();
    }
}

