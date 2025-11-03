package com.devstack.pos.util;

public class UserSessionData {
    public static String email = "";
    public static String jwtToken = "";
    public static String userRole = "";
    
    public static boolean isAdmin() {
        return "ROLE_SUPER_ADMIN".equalsIgnoreCase(userRole) || 
               "SUPER_ADMIN".equalsIgnoreCase(userRole) ||
               "ADMIN".equalsIgnoreCase(userRole);
    }
    
    public static boolean isCashier() {
        return "ROLE_CASHIER".equalsIgnoreCase(userRole) || 
               "CASHIER".equalsIgnoreCase(userRole);
    }
    
    public static void clear() {
        email = "";
        jwtToken = "";
        userRole = "";
    }
}
