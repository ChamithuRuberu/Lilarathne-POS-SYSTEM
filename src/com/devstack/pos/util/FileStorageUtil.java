package com.devstack.pos.util;

import java.io.File;

/**
 * Utility class for managing default file storage locations
 * All files are saved in Documents/POS_System/ with organized folder structure
 */
public class FileStorageUtil {
    
    private static final String BASE_FOLDER_NAME = "POS_System";
    
    /**
     * Get the base directory for all POS system files
     * Location: Documents/POS_System/
     * 
     * @return Base directory path
     */
    public static String getBaseDirectory() {
        String userHome = System.getProperty("user.home");
        String documentsPath = userHome + File.separator + "Documents" + File.separator + BASE_FOLDER_NAME;
        
        // Create directory if it doesn't exist
        File baseDir = new File(documentsPath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        
        return documentsPath;
    }
    
    /**
     * Get directory for regular order receipts
     * Location: Documents/POS_System/Receipts/Regular/[CashierName]/[CustomerName]/
     * 
     * @param cashierName Cashier/operator name
     * @param customerName Customer name
     * @return Receipt directory path
     */
    public static String getRegularReceiptDirectory(String cashierName, String customerName) {
        String baseDir = getBaseDirectory();
        String receiptsDir = baseDir + File.separator + "Receipts" + File.separator + "Regular" + 
                            File.separator + sanitizeFileName(cashierName) + File.separator + sanitizeFileName(customerName);
        
        File dir = new File(receiptsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        return receiptsDir;
    }
    
    /**
     * Get directory for super admin order receipts
     * Location: Documents/POS_System/SuperAdmin_Receipts/[OperatorName]/[CustomerName]/
     * Separate location from regular receipts for better organization
     * 
     * @param operatorName Operator name
     * @param customerName Customer name
     * @return Receipt directory path
     */
    public static String getSuperAdminReceiptDirectory(String operatorName, String customerName) {
        String baseDir = getBaseDirectory();
        String receiptsDir = baseDir + File.separator + "SuperAdmin_Receipts" + 
                            File.separator + sanitizeFileName(operatorName) + File.separator + sanitizeFileName(customerName);
        
        File dir = new File(receiptsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        return receiptsDir;
    }
    
    /**
     * Get directory for super admin reports
     * Location: Documents/POS_System/SuperAdmin_Reports/[ReportType]/
     * Separate location from regular reports for super admin
     * 
     * @param reportType Type of report (e.g., "Sales", "Inventory", "Financial")
     * @return Report directory path
     */
    public static String getSuperAdminReportDirectory(String reportType) {
        String baseDir = getBaseDirectory();
        String reportsDir = baseDir + File.separator + "SuperAdmin_Reports" + File.separator + sanitizeFileName(reportType);
        
        File dir = new File(reportsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        return reportsDir;
    }
    
    /**
     * Get directory for reports
     * Location: Documents/POS_System/Reports/[ReportType]/
     * 
     * @param reportType Type of report (e.g., "Sales", "Inventory", "Financial")
     * @return Report directory path
     */
    public static String getReportDirectory(String reportType) {
        String baseDir = getBaseDirectory();
        String reportsDir = baseDir + File.separator + "Reports" + File.separator + sanitizeFileName(reportType);
        
        File dir = new File(reportsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        return reportsDir;
    }
    
    /**
     * Get directory for exports
     * Location: Documents/POS_System/Exports/
     * 
     * @return Export directory path
     */
    public static String getExportDirectory() {
        String baseDir = getBaseDirectory();
        String exportsDir = baseDir + File.separator + "Exports";
        
        File dir = new File(exportsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        return exportsDir;
    }
    
    /**
     * Get directory for backups
     * Location: Documents/POS_System/Backups/
     * 
     * @return Backup directory path
     */
    public static String getBackupDirectory() {
        String baseDir = getBaseDirectory();
        String backupsDir = baseDir + File.separator + "Backups";
        
        File dir = new File(backupsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        return backupsDir;
    }
    
    /**
     * Get directory for logs
     * Location: Documents/POS_System/Logs/
     * 
     * @return Log directory path
     */
    public static String getLogDirectory() {
        String baseDir = getBaseDirectory();
        String logsDir = baseDir + File.separator + "Logs";
        
        File dir = new File(logsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        return logsDir;
    }
    
    /**
     * Sanitize file/folder name to remove invalid characters
     * 
     * @param name Original name
     * @return Sanitized name safe for file system
     */
    public static String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Unknown";
        }
        // Replace invalid characters with underscore
        return name.replaceAll("[^a-zA-Z0-9_\\-\\s]", "_").trim();
    }
    
    /**
     * Get full file path for a receipt
     * 
     * @param directory Directory path
     * @param fileName File name
     * @return Full file path
     */
    public static String getFilePath(String directory, String fileName) {
        return directory + File.separator + fileName;
    }
}

