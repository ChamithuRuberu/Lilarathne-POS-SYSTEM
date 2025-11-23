package com.devstack.pos.util;

import java.time.LocalDateTime;

public class UserSessionData {
    public static String email = "";
    public static String jwtToken = "";
    public static String userRole = "";
    public static LocalDateTime lastActivityTime = null;

    public static boolean isSuperAdmin() {
        return "ROLE_SUPER_ADMIN".equalsIgnoreCase(userRole) ||
                "SUPER_ADMIN".equalsIgnoreCase(userRole);
    }

    public static boolean isAdmin() {
        return "ROLE_ADMIN".equalsIgnoreCase(userRole) ||
                "ADMIN".equalsIgnoreCase(userRole);
    }

    public static boolean isCashier() {
        return "ROLE_CASHIER".equalsIgnoreCase(userRole) ||
                "CASHIER".equalsIgnoreCase(userRole);
    }

    /**
     * Update the last activity time to current time
     */
    public static void updateLastActivity() {
        lastActivityTime = LocalDateTime.now();
    }

    /**
     * Get the last activity time
     *
     * @return Last activity time, or null if never set
     */
    public static LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }

    /**
     * Check if user has been inactive for more than specified hours
     *
     * @param hours Number of hours to check
     * @return true if inactive for more than specified hours
     */
    public static boolean isInactiveForHours(long hours) {
        if (lastActivityTime == null) {
            return true; // No activity recorded, consider inactive
        }
        LocalDateTime now = LocalDateTime.now();
        return lastActivityTime.plusHours(hours).isBefore(now);
    }

    public static void clear() {
        email = "";
        jwtToken = "";
        userRole = "";
        lastActivityTime = null;
    }
}
