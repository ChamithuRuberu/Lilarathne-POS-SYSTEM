package com.devstack.pos.service;

import com.devstack.pos.util.JwtUtil;
import com.devstack.pos.util.StageManager;
import com.devstack.pos.util.UserSessionData;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Service to manage user session, monitor inactivity, and handle auto-logout
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManager {
    
    private final JwtUtil jwtUtil;
    private final TrialService trialService;
    private Timer inactivityTimer;
    
    @Value("${session.inactivity.timeout.hours:3}")
    private long inactivityTimeoutHours; // Configurable inactivity timeout in hours (default: 3)
    
    private static final long CHECK_INTERVAL_SECONDS = 60; // Check every minute
    private static final long TRIAL_WARNING_INTERVAL_MINUTES = 30; // Show trial warning every 30 minutes
    private long lastTrialWarningTime = 0; // Track last time warning was shown
    
    /**
     * Start monitoring user session for inactivity and token expiration
     */
    public void startSessionMonitoring() {
        log.info("Starting session monitoring for user: {} with inactivity timeout: {} hours", 
                UserSessionData.email, inactivityTimeoutHours);
        
        // Update last activity time when session starts
        UserSessionData.updateLastActivity();
        
        // Reset trial warning timer
        lastTrialWarningTime = 0;
        
        // Stop any existing timer
        stopSessionMonitoring();
        
        // Create new timer to check periodically
        inactivityTimer = new Timer("SessionMonitor", true);
        
        inactivityTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    checkSessionValidity();
                });
            }
        }, CHECK_INTERVAL_SECONDS * 1000, CHECK_INTERVAL_SECONDS * 1000);
    }
    
    /**
     * Stop monitoring user session
     */
    public void stopSessionMonitoring() {
        if (inactivityTimer != null) {
            inactivityTimer.cancel();
            inactivityTimer.purge();
            inactivityTimer = null;
            log.info("Stopped session monitoring");
        }
    }
    
    /**
     * Check if session is still valid (not expired and not inactive)
     */
    private void checkSessionValidity() {
        // Check if user is logged in
        if (UserSessionData.jwtToken == null || UserSessionData.jwtToken.isEmpty()) {
            return; // No active session
        }
        
        boolean shouldLogout = false;
        String logoutReason = "";
        
        // Check trial version expiration first
        if (trialService.isTrialEnabled() && trialService.isTrialExpired()) {
            shouldLogout = true;
            logoutReason = "Your trial period has expired. Please contact the administrator to extend or activate the full version.\n\n" +
                          "Trial End Date: " + trialService.getTrialEndDateFormatted();
            log.warn("Trial period expired for user: {}", UserSessionData.email);
        }
        // Check JWT token expiration
        else if (jwtUtil.isTokenExpired(UserSessionData.jwtToken)) {
            shouldLogout = true;
            logoutReason = "Your session has expired due to token expiration. Please login again.";
            log.warn("JWT token expired for user: {}", UserSessionData.email);
        }
        // Check inactivity timeout (configurable)
        else if (UserSessionData.isInactiveForHours(inactivityTimeoutHours)) {
            shouldLogout = true;
            String hoursText = inactivityTimeoutHours == 1 ? "hour" : "hours";
            logoutReason = String.format("Your session has expired due to inactivity (%d %s). Please login again.", 
                    inactivityTimeoutHours, hoursText);
            log.warn("User inactive for more than {} hours: {}", inactivityTimeoutHours, UserSessionData.email);
        }
        
        if (shouldLogout) {
            performAutoLogout(logoutReason);
        } else {
            // Check and show trial warning if 7 days or less remaining (show every 30 minutes)
            checkAndShowTrialWarning();
        }
    }
    
    /**
     * Check and show trial warning if 7 days or less remaining
     */
    private void checkAndShowTrialWarning() {
        if (trialService.isTrialActive() && trialService.isTrialWarningPeriod()) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastWarning = (currentTime - lastTrialWarningTime) / (1000 * 60); // minutes
            
            // Show warning every 30 minutes or on first check
            if (lastTrialWarningTime == 0 || timeSinceLastWarning >= TRIAL_WARNING_INTERVAL_MINUTES) {
                lastTrialWarningTime = currentTime;
                
                Platform.runLater(() -> {
                    try {
                        long daysRemaining = trialService.getDaysRemaining();
                        javafx.scene.control.Alert trialWarning = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                        trialWarning.setTitle("Trial Version Warning");
                        trialWarning.setHeaderText("⚠️ Trial Period Ending Soon");
                        
                        String message;
                        if (daysRemaining == 0) {
                            message = "⚠️ WARNING: Your trial period expires TODAY!\n\n" +
                                     "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n\n" +
                                     "Please contact the administrator to extend or activate the full version to avoid service interruption.";
                        } else if (daysRemaining == 1) {
                            message = "⚠️ WARNING: Your trial period expires TOMORROW!\n\n" +
                                     "Days remaining: " + daysRemaining + " day\n" +
                                     "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n\n" +
                                     "Please contact the administrator to extend or activate the full version.";
                        } else {
                            message = "⚠️ WARNING: Your trial period is ending soon!\n\n" +
                                     "Days remaining: " + daysRemaining + " days\n" +
                                     "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n\n" +
                                     "Please contact the administrator to extend or activate the full version before the trial expires.";
                        }
                        
                        trialWarning.setContentText(message);
                        trialWarning.show(); // Non-blocking show
                    } catch (Exception e) {
                        log.error("Error showing trial warning", e);
                    }
                });
            }
        }
    }
    
    /**
     * Perform automatic logout
     */
    private void performAutoLogout(String reason) {
        log.info("Performing auto-logout for user: {}", UserSessionData.email);
        
        // Stop monitoring
        stopSessionMonitoring();
        
        // Show alert to user and handle navigation
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Session Expired");
                alert.setHeaderText("Your session has expired");
                alert.setContentText(reason);
                
                // Show alert and wait for user response
                alert.showAndWait();
                
                // Clear session data after alert is closed (regardless of how it was closed)
                UserSessionData.clear();
                
                // Navigate to login screen
                navigateToLoginScreen();
                
            } catch (Exception e) {
                log.error("Error showing session expired alert", e);
                // Fallback: clear session and try to navigate
                try {
                    UserSessionData.clear();
                    navigateToLoginScreen();
                } catch (Exception ex) {
                    log.error("Failed to navigate to login screen in fallback", ex);
                }
            }
        });
    }
    
    /**
     * Navigate to login screen (helper method)
     */
    private void navigateToLoginScreen() {
        try {
            // Get all open windows and find a valid stage
            Stage primaryStage = null;
            for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                if (window instanceof Stage) {
                    Stage stage = (Stage) window;
                    if (stage.isShowing() && !stage.isIconified()) {
                        primaryStage = stage;
                        break;
                    }
                }
            }
            
            // If no valid stage found, try to get primary stage
            if (primaryStage == null) {
                primaryStage = (Stage) javafx.stage.Window.getWindows().stream()
                    .filter(Stage.class::isInstance)
                    .map(Stage.class::cast)
                    .filter(stage -> stage.isShowing())
                    .findFirst()
                    .orElse(null);
            }
            
            if (primaryStage != null && primaryStage.isShowing()) {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader();
                loader.setLocation(getClass().getResource("/com/devstack/pos/view/LoginForm.fxml"));
                loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
                
                javafx.scene.Parent root = loader.load();
                StageManager.loadAuthScene(primaryStage, root);
            } else {
                log.warn("No valid stage found for navigation to login screen");
                // Try to create a new stage as last resort
                Platform.runLater(() -> {
                    try {
                        Stage newStage = new Stage();
                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader();
                        loader.setLocation(getClass().getResource("/com/devstack/pos/view/LoginForm.fxml"));
                        loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
                        
                        javafx.scene.Parent root = loader.load();
                        StageManager.loadAuthScene(newStage, root);
                        newStage.show();
                    } catch (Exception e) {
                        log.error("Failed to create new stage for login screen", e);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to navigate to login screen during auto-logout", e);
        }
    }
    
    /**
     * Update last activity time (called when user performs any action)
     */
    public void updateActivity() {
        UserSessionData.updateLastActivity();
    }
    
    /**
     * Check if session is currently valid (for manual checks)
     * @return true if session is valid, false otherwise
     */
    public boolean isSessionValid() {
        if (UserSessionData.jwtToken == null || UserSessionData.jwtToken.isEmpty()) {
            return false;
        }
        
        // Check trial expiration
        if (trialService.isTrialEnabled() && trialService.isTrialExpired()) {
            return false;
        }
        
        // Check token expiration
        if (jwtUtil.isTokenExpired(UserSessionData.jwtToken)) {
            return false;
        }
        
        // Check inactivity
        if (UserSessionData.isInactiveForHours(inactivityTimeoutHours)) {
            return false;
        }
        
        return true;
    }
}

