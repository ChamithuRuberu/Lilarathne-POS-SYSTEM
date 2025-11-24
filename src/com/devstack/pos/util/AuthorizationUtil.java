package com.devstack.pos.util;

import javafx.scene.control.Alert;

public class AuthorizationUtil {
    
    /**
     * Check if current user has admin role
     * Supports: ROLE_SUPER_ADMIN, SUPER_ADMIN, ADMIN
     */
    public static boolean isAdmin() {
        return UserSessionData.isAdmin();
    }
    
    /**
     * Check if current user has cashier role
     * Supports: ROLE_CASHIER, CASHIER
     */
    public static boolean isCashier() {
        return UserSessionData.isCashier();
    }
    
    /**
     * Check if user can access POS Orders (Place Orders)
     * Accessible by: ADMIN and CASHIER
     */
    public static boolean canAccessPOSOrders() {
        return isAdmin() || isCashier();
    }
    
    /**
     * Check if user can access Return Orders
     * Accessible by: ADMIN and CASHIER
     */
    public static boolean canAccessReturnOrders() {
        return isAdmin() || isCashier();
    }
    
    /**
     * Check if user can access Purchase Orders
     * Accessible by: ADMIN only
     */
    public static boolean canAccessPurchaseOrders() {
        return isAdmin();
    }
    
    /**
     * Check if user can access all features
     * Accessible by: ADMIN only
     */
    public static boolean canAccessAllFeatures() {
        return isAdmin();
    }
    
    /**
     * Check if user can access customers management
     * Accessible by: ADMIN and CASHIER
     */
    public static boolean canAccessCustomers() {
        return isAdmin() || isCashier();
    }
    
    /**
     * Check if user can access products management
     * Accessible by: ADMIN only
     */
    public static boolean canAccessProducts() {
        return isAdmin();
    }
    
    /**
     * Check if user can access reports/analysis
     * Accessible by: ADMIN only
     */
    public static boolean canAccessReports() {
        return isAdmin();
    }
    
    /**
     * Check if user can access settings
     * Accessible by: ADMIN only
     */
    public static boolean canAccessSettings() {
        return isAdmin();
    }
    
    /**
     * Check if user can access General Items
     * Accessible by: SUPER_ADMIN only
     */
    public static boolean canAccessGeneralItems() {
        return UserSessionData.isSuperAdmin();
    }
    
    /**
     * Show unauthorized access alert
     */
    public static void showUnauthorizedAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Access Denied");
        alert.setHeaderText("Unauthorized Access");
        alert.setContentText("You don't have permission to access this feature.\n" +
                           "Current Role: " + UserSessionData.userRole);
        alert.showAndWait();
    }
    
    /**
     * Show admin only alert
     */
    public static void showAdminOnlyAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Admin Access Required");
        alert.setHeaderText("Admin Only Feature");
        alert.setContentText("This feature is only accessible by administrators.\n" +
                           "Your Role: " + UserSessionData.userRole);
        alert.showAndWait();
    }
    
    /**
     * Check authorization and show alert if denied
     * @return true if authorized, false otherwise
     */
    public static boolean checkAndAlert(boolean isAuthorized) {
        if (!isAuthorized) {
            showUnauthorizedAlert();
        }
        return isAuthorized;
    }
}

